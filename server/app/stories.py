import hashlib
import secrets
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import delete, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from .database import get_db
from .dependencies import current_user
from .models import Story, StoryCollaborator, StoryShareLink, User, utcnow
from .schemas import (
    CollaboratorResponse,
    UpdateCollaboratorRequest,
    CreateShareLinkRequest,
    ShareLinkResponse,
    StoryResponse,
    StoryUpsertRequest,
)

from .permissions import effective, validate_custom

router = APIRouter(tags=["stories"])


def share_token_hash(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def story_out(story: Story, access_role: str) -> StoryResponse:
    return StoryResponse(
        id=story.id,
        revision=story.revision,
        payload=story.payload,
        updatedAt=story.updated_at,
        accessRole=access_role,
    )


async def access_role_for(
    db: AsyncSession,
    story: Story,
    user: User,
) -> str | None:
    if story.owner_id == user.id:
        return "owner"

    return await db.scalar(
        select(StoryCollaborator.role).where(
            StoryCollaborator.story_id == story.id,
            StoryCollaborator.user_id == user.id,
        )
    )


async def get_story_with_access(
    db: AsyncSession,
    story_id: UUID,
    user: User,
) -> tuple[Story, str]:
    story = await db.scalar(select(Story).where(Story.id == story_id))
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    role = await access_role_for(db, story, user)
    if role is None:
        # Не раскрываем существование чужой истории пользователю без доступа.
        raise HTTPException(status_code=404, detail="Story not found")

    return story, role


@router.get("/stories", response_model=list[StoryResponse])
async def list_stories(
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    collaborator_rows = (
        await db.execute(
            select(StoryCollaborator.story_id, StoryCollaborator.role).where(
                StoryCollaborator.user_id == user.id
            )
        )
    ).all()
    collaborator_roles = {row.story_id: row.role for row in collaborator_rows}
    collaborator_ids = list(collaborator_roles.keys())

    predicate = Story.owner_id == user.id
    if collaborator_ids:
        predicate = or_(predicate, Story.id.in_(collaborator_ids))

    rows = (
        await db.scalars(
            select(Story)
            .where(predicate)
            .order_by(Story.updated_at.desc())
        )
    ).all()

    return [
        story_out(
            story,
            "owner" if story.owner_id == user.id else collaborator_roles[story.id],
        )
        for story in rows
    ]


@router.post("/stories", response_model=StoryResponse, status_code=status.HTTP_201_CREATED)
async def create_story(
    body: StoryUpsertRequest,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = Story(
        owner_id=user.id,
        title=body.payload.title,
        payload=body.payload.model_dump(mode="json"),
        revision=1,
    )
    db.add(story)
    await db.commit()
    await db.refresh(story)
    return story_out(story, "owner")


def _payload_changes_allowed(old: dict, new: dict, permissions: dict) -> bool:
    # Whole-document PUT: compare protected sections to prevent bypassing UI toggles.
    if old.get("nodes") != new.get("nodes"):
        before = {n["clientId"]: n for n in old.get("nodes", [])}
        after = {n["clientId"]: n for n in new.get("nodes", [])}
        if before.keys() != after.keys() and permissions["blocks"] != "full":
            return False
        for node_id in before.keys() & after.keys():
            a, b = before[node_id], after[node_id]
            if (a.get("positionX"), a.get("positionY")) != (b.get("positionX"), b.get("positionY")):
                if permissions["blocks"] not in {"move", "edit", "full"}: return False
            if {k:v for k,v in a.items() if k not in {"positionX","positionY"}} != {k:v for k,v in b.items() if k not in {"positionX","positionY"}}:
                if permissions["blocks"] not in {"edit", "full"}: return False
    if old.get("connections") != new.get("connections"):
        before = {n["clientId"]: n for n in old.get("connections", [])}
        after = {n["clientId"]: n for n in new.get("connections", [])}
        if before.keys() != after.keys() and permissions["lines"] != "full": return False
        for line_id in before.keys() & after.keys():
            if before[line_id] != after[line_id] and permissions["lines"] not in {"edit", "full"}: return False
    for field in ("manuscript", "wordDocument", "characters", "description"):
        if old.get(field) != new.get(field) and permissions["script"] not in {"edit", "full"}: return False
    if old.get("title") != new.get("title") and permissions["script"] != "full": return False
    return True


@router.put("/stories/{story_id}", response_model=StoryResponse)
async def update_story(
    story_id: UUID,
    body: StoryUpsertRequest,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story, role = await get_story_with_access(db, story_id, user)
    if role not in {"owner", "admin", "editor", "reader"}:
        raise HTTPException(status_code=403, detail="No access")
    if role != "owner":
        collaborator = await db.scalar(select(StoryCollaborator).where(StoryCollaborator.story_id == story_id, StoryCollaborator.user_id == user.id))
        perms = effective(role, collaborator.permissions)
        if not _payload_changes_allowed(story.payload, body.payload.model_dump(mode="json"), perms):
            raise HTTPException(status_code=403, detail="Insufficient edit permissions")
    if role not in {"owner", "admin", "editor"}:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail={"code": "story_read_only", "message": "Reader access is read-only"},
        )

    if body.baseRevision is not None and body.baseRevision != story.revision:
        raise HTTPException(
            status_code=409,
            detail={
                "code": "story_revision_conflict",
                "serverRevision": story.revision,
            },
        )

    story.title = body.payload.title
    story.payload = body.payload.model_dump(mode="json")
    story.revision += 1
    await db.commit()
    await db.refresh(story)
    return story_out(story, role)


@router.delete("/stories/{story_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_story(
    story_id: UUID,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = await db.scalar(
        select(Story).where(Story.id == story_id, Story.owner_id == user.id)
    )
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")
    await db.delete(story)
    await db.commit()


@router.post(
    "/stories/{story_id}/share-links",
    response_model=ShareLinkResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_share_link(
    story_id: UUID,
    body: CreateShareLinkRequest,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = await db.scalar(
        select(Story).where(Story.id == story_id, Story.owner_id == user.id)
    )
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    token = secrets.token_urlsafe(32)
    link = StoryShareLink(
        story_id=story.id,
        created_by=user.id,
        token_hash=share_token_hash(token),
        role=body.role,
    )
    db.add(link)
    await db.commit()
    await db.refresh(link)

    return ShareLinkResponse(
        id=link.id,
        role=link.role,
        deepLink=f"novelforge://share/{token}",
        createdAt=link.created_at,
    )


@router.delete(
    "/stories/{story_id}/share-links/{link_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def revoke_share_link(
    story_id: UUID,
    link_id: UUID,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = await db.scalar(
        select(Story).where(Story.id == story_id, Story.owner_id == user.id)
    )
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    link = await db.scalar(
        select(StoryShareLink).where(
            StoryShareLink.id == link_id,
            StoryShareLink.story_id == story_id,
            StoryShareLink.revoked_at.is_(None),
        )
    )
    if not link:
        raise HTTPException(status_code=404, detail="Share link not found")

    link.revoked_at = utcnow()
    await db.commit()


@router.post("/share-links/{token}/accept", response_model=StoryResponse)
async def accept_share_link(
    token: str,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    if len(token) < 20 or len(token) > 200:
        raise HTTPException(status_code=404, detail="Share link not found")

    link = await db.scalar(
        select(StoryShareLink).where(
            StoryShareLink.token_hash == share_token_hash(token),
            StoryShareLink.revoked_at.is_(None),
        )
    )
    if not link:
        raise HTTPException(status_code=404, detail="Share link not found")

    story = await db.scalar(select(Story).where(Story.id == link.story_id))
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    if story.owner_id == user.id:
        return story_out(story, "owner")

    collaborator = await db.scalar(
        select(StoryCollaborator).where(
            StoryCollaborator.story_id == story.id,
            StoryCollaborator.user_id == user.id,
        )
    )
    if collaborator is None:
        collaborator = StoryCollaborator(
            story_id=story.id,
            user_id=user.id,
            role=link.role,
        )
        db.add(collaborator)
    else:
        collaborator.role = link.role

    await db.commit()
    return story_out(story, link.role)


@router.get(
    "/stories/{story_id}/collaborators",
    response_model=list[CollaboratorResponse],
)
async def list_collaborators(
    story_id: UUID,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story, _ = await get_story_with_access(db, story_id, user)

    rows = (
        await db.execute(
            select(StoryCollaborator, User)
            .join(User, User.id == StoryCollaborator.user_id)
            .where(StoryCollaborator.story_id == story_id)
            .order_by(User.email)
        )
    ).all()

    owner = await db.get(User, story.owner_id)
    result = [CollaboratorResponse(userId=owner.id, email=owner.email, login=owner.login, displayName=owner.login, role="owner", permissions=effective("owner"))]
    result.extend([CollaboratorResponse(
            userId=collaborator.user_id,
            email=collaborator_user.email,
            login=collaborator_user.login,
            displayName=collaborator_user.login,
            role=collaborator.role,
            permissions=effective(collaborator.role, collaborator.permissions),
        )
        for collaborator, collaborator_user in rows
    ])
    return result


@router.delete(
    "/stories/{story_id}/collaborators/{collaborator_user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def remove_collaborator(
    story_id: UUID,
    collaborator_user_id: UUID,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = await db.scalar(
        select(Story).where(Story.id == story_id, Story.owner_id == user.id)
    )
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

    await db.execute(
        delete(StoryCollaborator).where(
            StoryCollaborator.story_id == story_id,
            StoryCollaborator.user_id == collaborator_user_id,
        )
    )
    await db.commit()


@router.patch("/stories/{story_id}/collaborators/{collaborator_user_id}", response_model=CollaboratorResponse)
async def update_collaborator(
    story_id: UUID,
    collaborator_user_id: UUID,
    body: UpdateCollaboratorRequest,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story, actor_role = await get_story_with_access(db, story_id, user)
    if collaborator_user_id == story.owner_id:
        raise HTTPException(status_code=403, detail="Owner cannot be modified")
    target = await db.scalar(select(StoryCollaborator).where(StoryCollaborator.story_id == story_id, StoryCollaborator.user_id == collaborator_user_id))
    if target is None:
        raise HTTPException(status_code=404, detail="Collaborator not found")
    if actor_role != "owner":
        actor = await db.scalar(select(StoryCollaborator).where(StoryCollaborator.story_id == story_id, StoryCollaborator.user_id == user.id))
        if actor_role != "admin" or effective(actor_role, actor.permissions)["members"] not in {"manage", "full"}:
            raise HTTPException(status_code=403, detail="Cannot manage members")
        if target.role == "admin" or body.role == "admin":
            raise HTTPException(status_code=403, detail="Only owner can manage admins")
    custom = body.permissions.model_dump()
    try:
        validate_custom(body.role, custom)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    target.role = body.role
    target.permissions = custom
    await db.commit()
    member = await db.get(User, target.user_id)
    return CollaboratorResponse(userId=member.id, email=member.email, login=member.login, displayName=member.login, role=target.role, permissions=effective(target.role, target.permissions))
