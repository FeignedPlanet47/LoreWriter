import asyncio
from datetime import datetime, timedelta, timezone
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from .config import settings
from .database import get_db
from .dependencies import current_user
from .mailer import send_verification_email
from .models import EmailVerificationCode, RefreshToken, User
from .schemas import (
    AuthResponse,
    LoginRequest,
    LogoutRequest,
    RefreshRequest,
    RegisterRequest,
    ResendVerificationRequest,
    UserOut,
    VerificationPendingResponse,
    VerifyEmailRequest,
)
from .security import (
    create_access_token,
    dummy_verify,
    email_verification_expiry,
    hash_email_verification_code,
    hash_password,
    hash_refresh_token,
    new_email_verification_code,
    new_refresh_token,
    refresh_expiry,
    verify_email_verification_code,
    verify_password,
)

router = APIRouter(prefix="/auth", tags=["auth"])


def user_out(user: User) -> UserOut:
    return UserOut(
        id=user.id,
        email=user.email,
        displayName=user.display_name,
        emailVerified=user.is_verified,
    )


async def issue_tokens(db: AsyncSession, user: User) -> AuthResponse:
    if not user.is_verified:
        raise HTTPException(status_code=403, detail="EMAIL_NOT_VERIFIED")

    raw_refresh = new_refresh_token()
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=hash_refresh_token(raw_refresh),
            expires_at=refresh_expiry(),
        )
    )
    await db.commit()
    return AuthResponse(
        accessToken=create_access_token(user.id),
        refreshToken=raw_refresh,
        user=user_out(user),
    )


async def create_and_send_verification_code(
    db: AsyncSession,
    user: User,
    *,
    enforce_rate_limits: bool,
) -> int:
    now = datetime.now(timezone.utc)

    if enforce_rate_limits:
        latest = await db.scalar(
            select(EmailVerificationCode)
            .where(EmailVerificationCode.user_id == user.id)
            .order_by(EmailVerificationCode.created_at.desc())
            .limit(1)
        )
        if latest:
            elapsed = (now - latest.created_at).total_seconds()
            remaining = settings.email_verification_resend_seconds - int(elapsed)
            if remaining > 0:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="VERIFICATION_RESEND_TOO_SOON",
                    headers={"Retry-After": str(remaining)},
                )

        hour_ago = now - timedelta(hours=1)
        sends_last_hour = await db.scalar(
            select(func.count(EmailVerificationCode.id)).where(
                EmailVerificationCode.user_id == user.id,
                EmailVerificationCode.created_at >= hour_ago,
            )
        )
        if int(sends_last_hour or 0) >= settings.email_verification_max_sends_per_hour:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="VERIFICATION_SEND_LIMIT_REACHED",
                headers={"Retry-After": "3600"},
            )

    # Только последний код должен оставаться действующим.
    await db.execute(
        update(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id,
            EmailVerificationCode.consumed_at.is_(None),
        )
        .values(consumed_at=now)
    )

    raw_code = new_email_verification_code()
    code = EmailVerificationCode(
        user_id=user.id,
        code_hash=hash_email_verification_code(user.id, raw_code),
        expires_at=email_verification_expiry(),
    )
    db.add(code)
    await db.flush()

    try:
        await asyncio.to_thread(
            send_verification_email,
            user.email,
            user.display_name,
            raw_code,
        )
    except Exception:
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="EMAIL_SEND_FAILED",
        )

    return settings.email_verification_resend_seconds


@router.post(
    "/register",
    response_model=VerificationPendingResponse,
    status_code=status.HTTP_201_CREATED,
)
async def register(body: RegisterRequest, db: Annotated[AsyncSession, Depends(get_db)]):
    email = body.email.lower().strip()
    existing = await db.scalar(select(User).where(User.email == email))
    if existing:
        raise HTTPException(status_code=409, detail="EMAIL_ALREADY_REGISTERED")

    user = User(
        email=email,
        display_name=body.displayName.strip(),
        password_hash=hash_password(body.password),
        is_verified=False,
    )
    db.add(user)

    try:
        await db.flush()
        resend_after = await create_and_send_verification_code(
            db,
            user,
            enforce_rate_limits=False,
        )
        await db.commit()
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status_code=409, detail="EMAIL_ALREADY_REGISTERED")

    return VerificationPendingResponse(
        email=user.email,
        resendAfterSeconds=resend_after,
    )


@router.post("/verify-email", response_model=AuthResponse)
async def verify_email(
    body: VerifyEmailRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
):
    now = datetime.now(timezone.utc)
    email = body.email.lower().strip()
    user = await db.scalar(
        select(User)
        .where(User.email == email, User.is_active.is_(True))
        .with_for_update()
    )
    if not user:
        raise HTTPException(status_code=400, detail="INVALID_VERIFICATION_CODE")

    if user.is_verified:
        return await issue_tokens(db, user)

    code = await db.scalar(
        select(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id,
            EmailVerificationCode.consumed_at.is_(None),
        )
        .order_by(EmailVerificationCode.created_at.desc())
        .limit(1)
        .with_for_update()
    )
    if not code:
        raise HTTPException(status_code=400, detail="INVALID_VERIFICATION_CODE")

    if code.expires_at <= now:
        code.consumed_at = now
        await db.commit()
        raise HTTPException(status_code=400, detail="VERIFICATION_CODE_EXPIRED")

    if code.attempts >= settings.email_verification_max_attempts:
        code.consumed_at = now
        await db.commit()
        raise HTTPException(status_code=429, detail="TOO_MANY_VERIFICATION_ATTEMPTS")

    if not verify_email_verification_code(user.id, body.code, code.code_hash):
        code.attempts += 1
        if code.attempts >= settings.email_verification_max_attempts:
            code.consumed_at = now
            await db.commit()
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="TOO_MANY_VERIFICATION_ATTEMPTS",
            )
        await db.commit()
        raise HTTPException(status_code=400, detail="INVALID_VERIFICATION_CODE")

    code.consumed_at = now
    user.is_verified = True
    user.verified_at = now

    await db.execute(
        update(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id,
            EmailVerificationCode.id != code.id,
            EmailVerificationCode.consumed_at.is_(None),
        )
        .values(consumed_at=now)
    )
    await db.commit()
    await db.refresh(user)

    return await issue_tokens(db, user)


@router.post("/resend-verification", response_model=VerificationPendingResponse)
async def resend_verification(
    body: ResendVerificationRequest,
    db: Annotated[AsyncSession, Depends(get_db)],
):
    email = body.email.lower().strip()
    user = await db.scalar(select(User).where(User.email == email, User.is_active.is_(True)))

    # Не раскрываем, существует ли адрес. Реальная отправка происходит только
    # для существующего неподтвержденного пользователя.
    if not user or user.is_verified:
        return VerificationPendingResponse(
            email=email,
            resendAfterSeconds=settings.email_verification_resend_seconds,
        )

    resend_after = await create_and_send_verification_code(
        db,
        user,
        enforce_rate_limits=True,
    )
    await db.commit()

    return VerificationPendingResponse(
        email=email,
        resendAfterSeconds=resend_after,
    )


@router.post("/login", response_model=AuthResponse)
async def login(body: LoginRequest, db: Annotated[AsyncSession, Depends(get_db)]):
    email = body.email.lower().strip()
    user = await db.scalar(select(User).where(User.email == email, User.is_active.is_(True)))
    if not user:
        dummy_verify(body.password)
        raise HTTPException(status_code=401, detail="INVALID_EMAIL_OR_PASSWORD")
    if not verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="INVALID_EMAIL_OR_PASSWORD")
    if not user.is_verified:
        raise HTTPException(status_code=403, detail="EMAIL_NOT_VERIFIED")
    return await issue_tokens(db, user)


@router.post("/refresh", response_model=AuthResponse)
async def refresh(body: RefreshRequest, db: Annotated[AsyncSession, Depends(get_db)]):
    now = datetime.now(timezone.utc)
    token = await db.scalar(
        select(RefreshToken)
        .where(
            RefreshToken.token_hash == hash_refresh_token(body.refreshToken),
            RefreshToken.revoked_at.is_(None),
            RefreshToken.expires_at > now,
        )
        .with_for_update()
    )
    if not token:
        raise HTTPException(status_code=401, detail="INVALID_REFRESH_TOKEN")

    user = await db.get(User, token.user_id)
    if not user or not user.is_active or not user.is_verified:
        raise HTTPException(status_code=401, detail="USER_UNAVAILABLE")

    token.revoked_at = now
    await db.flush()
    return await issue_tokens(db, user)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(body: LogoutRequest, db: Annotated[AsyncSession, Depends(get_db)]):
    token = await db.scalar(
        select(RefreshToken).where(RefreshToken.token_hash == hash_refresh_token(body.refreshToken))
    )
    if token and token.revoked_at is None:
        token.revoked_at = datetime.now(timezone.utc)
        await db.commit()


@router.get("/me", response_model=UserOut)
async def me(user: Annotated[User, Depends(current_user)]):
    return user_out(user)
