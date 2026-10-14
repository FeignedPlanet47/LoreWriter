import hashlib
import hmac
import secrets
from datetime import datetime, timedelta, timezone
from uuid import UUID

import jwt
from fastapi import HTTPException, status
from jwt.exceptions import InvalidTokenError
from pwdlib import PasswordHash

from .config import settings

password_hasher = PasswordHash.recommended()
DUMMY_HASH = password_hasher.hash("NovelForge-dummy-password")
ALGORITHM = "HS256"


def hash_password(password: str) -> str:
    return password_hasher.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    return password_hasher.verify(password, password_hash)


def dummy_verify(password: str) -> None:
    password_hasher.verify(password, DUMMY_HASH)


def create_access_token(user_id: UUID) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user_id),
        "type": "access",
        "iss": settings.jwt_issuer,
        "aud": settings.jwt_audience,
        "iat": now,
        "exp": now + timedelta(minutes=settings.access_token_minutes),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=ALGORITHM)


def decode_access_token(token: str) -> UUID:
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=[ALGORITHM],
            issuer=settings.jwt_issuer,
            audience=settings.jwt_audience,
        )
        if payload.get("type") != "access":
            raise ValueError("wrong token type")
        return UUID(payload["sub"])
    except (InvalidTokenError, ValueError, KeyError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired access token",
            headers={"WWW-Authenticate": "Bearer"},
        )


def new_refresh_token() -> str:
    return secrets.token_urlsafe(48)


def hash_refresh_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def refresh_expiry() -> datetime:
    return datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_days)


def new_email_verification_code() -> str:
    return f"{secrets.randbelow(1_000_000):06d}"


def hash_email_verification_code(user_id: UUID, code: str) -> str:
    message = f"{user_id}:{code}".encode("utf-8")
    return hmac.new(
        settings.email_code_secret.encode("utf-8"),
        message,
        hashlib.sha256,
    ).hexdigest()


def verify_email_verification_code(user_id: UUID, code: str, expected_hash: str) -> bool:
    actual = hash_email_verification_code(user_id, code)
    return hmac.compare_digest(actual, expected_hash)


def email_verification_expiry() -> datetime:
    return datetime.now(timezone.utc) + timedelta(
        minutes=settings.email_verification_code_minutes
    )
