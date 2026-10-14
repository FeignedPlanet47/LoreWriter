from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from .database import get_db
from .dependencies import current_user
from .models import Story, User
from .schemas import StoryResponse, StoryUpsertRequest

router = APIRouter(prefix="/stories", tags=["stories"])


def story_out(story: Story) -> StoryResponse:
    return StoryResponse(
        id=story.id,
        revision=story.revision,
        payload=story.payload,
        updatedAt=story.updated_at,
    )


@router.get("", response_model=list[StoryResponse])
async def list_stories(
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    rows = (
        await db.scalars(
            select(Story)
            .where(Story.owner_id == user.id)
            .order_by(Story.updated_at.desc())
        )
    ).all()
    return [story_out(row) for row in rows]


@router.post("", response_model=StoryResponse, status_code=status.HTTP_201_CREATED)
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
    return story_out(story)


@router.put("/{story_id}", response_model=StoryResponse)
async def update_story(
    story_id: UUID,
    body: StoryUpsertRequest,
    user: Annotated[User, Depends(current_user)],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    story = await db.scalar(
        select(Story).where(Story.id == story_id, Story.owner_id == user.id)
    )
    if not story:
        raise HTTPException(status_code=404, detail="Story not found")

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
    return story_out(story)


@router.delete("/{story_id}", status_code=status.HTTP_204_NO_CONTENT)
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
