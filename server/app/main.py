from fastapi import FastAPI, HTTPException
from sqlalchemy import text

from .auth import router as auth_router
from .config import settings
from .database import engine
from .stories import router as stories_router

is_development = settings.environment.lower() == "development"

app = FastAPI(
    title="NovelForge API",
    version="1.3.0",
    docs_url="/docs" if is_development else None,
    openapi_url="/openapi.json" if is_development else None,
    redoc_url=None,
)

app.include_router(auth_router, prefix="/api/v1")
app.include_router(stories_router, prefix="/api/v1")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/ready")
async def readiness():
    try:
        async with engine.connect() as connection:
            await connection.execute(text("SELECT 1"))
    except Exception as exc:
        raise HTTPException(status_code=503, detail="database_unavailable") from exc
    return {"status": "ready", "database": "ok"}
