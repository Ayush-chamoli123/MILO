"""
MILO Authentication API Endpoints
=================================
FastAPI routes for user registration, login, token refresh, logout, and profile queries.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.entities import User
from app.schemas.auth import (
    AuthResponse,
    PasswordChangeRequest,
    RefreshTokenRequest,
    TokenResponse,
    UserLoginRequest,
    UserRegisterRequest,
    UserResponse,
)
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["Authentication"])
security_scheme = HTTPBearer(auto_error=False)


def get_auth_service(db: Session = Depends(get_db)) -> AuthService:
    return AuthService(db=db)


def get_current_authenticated_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(security_scheme),
    auth_service: AuthService = Depends(get_auth_service),
) -> User:
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication credentials were not provided.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return auth_service.get_current_user(credentials.credentials)


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(
    request: UserRegisterRequest,
    auth_service: AuthService = Depends(get_auth_service),
) -> AuthResponse:
    """Register a new user account and obtain authentication tokens."""
    return auth_service.register(request)


@router.post("/login", response_model=AuthResponse)
def login(
    request: UserLoginRequest,
    auth_service: AuthService = Depends(get_auth_service),
) -> AuthResponse:
    """Authenticate with email/username and password to obtain JWT tokens."""
    return auth_service.login(request)


@router.post("/refresh", response_model=TokenResponse)
def refresh_token(
    request: RefreshTokenRequest,
    auth_service: AuthService = Depends(get_auth_service),
) -> TokenResponse:
    """Exchange a valid refresh token for a newly issued access token."""
    return auth_service.refresh_token(request)


@router.post("/logout", status_code=status.HTTP_200_OK)
def logout(
    request: RefreshTokenRequest,
    auth_service: AuthService = Depends(get_auth_service),
):
    """Revoke a refresh token and invalidate its active session."""
    success = auth_service.logout(request.refresh_token)
    return {"message": "Logged out successfully", "session_revoked": success}


@router.get("/me", response_model=UserResponse)
def get_current_user_profile(
    current_user: User = Depends(get_current_authenticated_user),
) -> UserResponse:
    """Get profile details of the currently authenticated user (excluding password_hash)."""
    return UserResponse.model_validate(current_user)


@router.post("/change-password", status_code=status.HTTP_200_OK)
def change_password(
    request: PasswordChangeRequest,
    current_user: User = Depends(get_current_authenticated_user),
    auth_service: AuthService = Depends(get_auth_service),
):
    """Update password for the logged-in user and invalidate previous sessions."""
    auth_service.change_password(current_user.user_id, request)
    return {"message": "Password updated successfully."}
