"""
MILO Application Configuration
==============================
Central settings management using Pydantic Settings.
Reads environment variables with sensible defaults for local development and testing.
"""

from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    PROJECT_NAME: str = "MILO"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"

    # Environment & Debug
    ENVIRONMENT: str = "development"
    DEBUG: bool = True

    # Database
    DATABASE_URL: str = "sqlite:///./milo.db"

    # JWT & Cryptographic Security
    JWT_SECRET_KEY: str = "super-secret-milo-development-key-change-in-production-123456789"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # CORS
    BACKEND_CORS_ORIGINS: list[str] = ["*"]

    # SMTP Email Configuration (Optional for real email dispatch)
    SMTP_HOST: str | None = None
    SMTP_PORT: int = 587
    SMTP_USER: str | None = None
    SMTP_PASSWORD: str | None = None
    SMTP_TLS: bool = True
    EMAILS_FROM_EMAIL: str = "noreply@milo.app"
    EMAILS_FROM_NAME: str = "MILO App"

    # SMS Gateway Configuration (Optional, e.g. Twilio)
    TWILIO_ACCOUNT_SID: str | None = None
    TWILIO_AUTH_TOKEN: str | None = None
    TWILIO_PHONE_NUMBER: str | None = None

    model_config = SettingsConfigDict(
        env_file=str(Path(__file__).resolve().parent.parent.parent / ".env"),
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )


settings = Settings()
