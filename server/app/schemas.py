from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field


StoryAccessRole = Literal["owner", "admin", "editor", "reader"]
ShareLinkRole = Literal["editor", "reader"]


class UserOut(BaseModel):
    id: UUID
    email: EmailStr
    login: str
    displayName: str = ""
    emailVerified: bool = True


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=10, max_length=128)
    login: str = Field(min_length=3, max_length=32, pattern=r"^[\w.-]+$")


class LoginRequest(BaseModel):
    login: str = Field(min_length=3, max_length=32, pattern=r"^[\w.-]+$")
    password: str = Field(min_length=1, max_length=128)


class VerifyEmailRequest(BaseModel):
    email: EmailStr
    code: str = Field(pattern=r"^\d{6}$")


class ResendVerificationRequest(BaseModel):
    email: EmailStr


class VerificationPendingResponse(BaseModel):
    email: EmailStr
    verificationRequired: bool = True
    resendAfterSeconds: int = 60


class RefreshRequest(BaseModel):
    refreshToken: str = Field(min_length=20)


class LogoutRequest(BaseModel):
    refreshToken: str = Field(min_length=20)


class AuthResponse(BaseModel):
    accessToken: str
    refreshToken: str
    tokenType: str = "bearer"
    user: UserOut


class TextFormatMarkPayload(BaseModel):
    start: int
    end: int
    type: str
    value: str | None = None


class StoryNodePayload(BaseModel):
    clientId: int
    title: str
    text: str = ""
    formatting: list[TextFormatMarkPayload] = Field(default_factory=list)
    type: str
    positionX: float
    positionY: float


class StoryConnectionPayload(BaseModel):
    clientId: int
    fromNodeClientId: int
    toNodeClientId: int
    label: str = ""
    sortOrder: int = 0


class CharacterPayload(BaseModel):
    clientId: int
    name: str
    role: str = ""
    description: str = ""
    personality: str = ""
    appearance: str = ""
    notes: str = ""


class StoryPayload(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = ""
    manuscript: str = ""
    wordDocument: str = ""
    createdAt: int
    updatedAt: int
    nodes: list[StoryNodePayload] = Field(default_factory=list)
    connections: list[StoryConnectionPayload] = Field(default_factory=list)
    characters: list[CharacterPayload] = Field(default_factory=list)


class StoryUpsertRequest(BaseModel):
    payload: StoryPayload
    baseRevision: int | None = Field(default=None, ge=0)


class StoryResponse(BaseModel):
    id: UUID
    revision: int
    payload: StoryPayload
    updatedAt: datetime
    accessRole: StoryAccessRole


class CreateShareLinkRequest(BaseModel):
    role: ShareLinkRole


class ShareLinkResponse(BaseModel):
    id: UUID
    role: ShareLinkRole
    deepLink: str
    createdAt: datetime


class CollaboratorPermissions(BaseModel):
    blocks: Literal["view", "move", "edit", "full"] = "view"
    lines: Literal["view", "move", "edit", "full"] = "view"
    script: Literal["read", "edit", "full"] = "read"
    members: Literal["none", "invite", "manage", "full"] = "none"
    export: Literal["none", "export", "share"] = "none"


class UpdateCollaboratorRequest(BaseModel):
    role: Literal["admin", "editor", "reader"]
    permissions: CollaboratorPermissions


class CollaboratorResponse(BaseModel):
    userId: UUID
    email: EmailStr
    login: str
    displayName: str
    role: StoryAccessRole
    permissions: CollaboratorPermissions
