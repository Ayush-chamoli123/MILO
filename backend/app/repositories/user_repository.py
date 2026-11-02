"""
MILO User Repository
====================
Encapsulates all database persistence and query operations for the User entity,
UserSession records, and Verification/OTP records.
Strictly implements requirements for AUTH-01 through AUTH-12:
- Unique lookup & existence checking for email, username, and phone number
- User creation with proper transactional commits/flushes
- Soft deletion preserving relational integrity
- Session tracking and token revocation checks
- Identity and OTP verification auditing (AUTH-09)
"""

from datetime import datetime, timezone
from typing import Sequence
from sqlalchemy import func, select, or_
from sqlalchemy.orm import Session

from app.models.entities import (
    User,
    UserSession,
    UserStatus,
    Verification,
    VerificationStatus,
    VerificationType,
)


class UserRepository:
    """Repository handling database operations for User, UserSession, and Verification entities."""

    def __init__(self, db: Session) -> None:
        self.db = db

    # ==========================================================================
    # USER QUERY METHODS
    # ==========================================================================

    def get_by_id(self, user_id: str, include_deleted: bool = False) -> User | None:
        """
        Retrieves a user by their primary key user_id (UUID).
        By default, filters out soft-deleted accounts.
        """
        query = select(User).where(User.user_id == user_id)
        if not include_deleted:
            query = query.where(User.deleted_at.is_(None))
        return self.db.execute(query).scalar_one_or_none()

    def get_by_email(self, email: str, include_deleted: bool = False) -> User | None:
        """
        Retrieves a user by exact normalized (lowercase) email address.
        """
        normalized = email.strip().lower()
        query = select(User).where(func.lower(User.email) == normalized)
        if not include_deleted:
            query = query.where(User.deleted_at.is_(None))
        return self.db.execute(query).scalar_one_or_none()

    def get_by_username(self, username: str, include_deleted: bool = False) -> User | None:
        """
        Retrieves a user by their unique username handle.
        """
        clean_handle = username.strip()
        query = select(User).where(User.username == clean_handle)
        if not include_deleted:
            query = query.where(User.deleted_at.is_(None))
        return self.db.execute(query).scalar_one_or_none()

    def get_by_phone(self, phone_number: str, include_deleted: bool = False) -> User | None:
        """
        Retrieves a user by their phone number (AUTH-03).
        """
        clean_phone = phone_number.strip()
        query = select(User).where(User.phone_number == clean_phone)
        if not include_deleted:
            query = query.where(User.deleted_at.is_(None))
        return self.db.execute(query).scalar_one_or_none()

    def get_by_email_or_username(
        self, identifier: str, include_deleted: bool = False
    ) -> User | None:
        """
        Finds a user matching either their email or username handle.
        Useful for flexible login credentials.
        """
        clean_id = identifier.strip()
        lower_id = clean_id.lower()
        query = select(User).where(
            or_(
                func.lower(User.email) == lower_id,
                User.username == clean_id,
            )
        )
        if not include_deleted:
            query = query.where(User.deleted_at.is_(None))
        return self.db.execute(query).scalar_one_or_none()

    # ==========================================================================
    # EXISTENCE CHECKS (FAST SCALARS)
    # ==========================================================================

    def exists_by_email(self, email: str) -> bool:
        """Checks if an email is already registered in the system (AUTH-02)."""
        normalized = email.strip().lower()
        query = select(User.user_id).where(func.lower(User.email) == normalized)
        return self.db.execute(query).first() is not None

    def exists_by_username(self, username: str) -> bool:
        """Checks if a username handle is already taken."""
        clean_handle = username.strip()
        query = select(User.user_id).where(User.username == clean_handle)
        return self.db.execute(query).first() is not None

    def exists_by_phone(self, phone_number: str) -> bool:
        """Checks if a phone number is already registered (AUTH-03)."""
        clean_phone = phone_number.strip()
        query = select(User.user_id).where(User.phone_number == clean_phone)
        return self.db.execute(query).first() is not None

    # ==========================================================================
    # MUTATION METHODS (CREATE / UPDATE / SOFT-DELETE)
    # ==========================================================================

    def create(self, user: User, auto_commit: bool = True) -> User:
        """
        Persists a newly created User entity to the database.
        Flushes or commits transaction and refreshes the instance attributes.
        """
        self.db.add(user)
        if auto_commit:
            self.db.commit()
            self.db.refresh(user)
        else:
            self.db.flush()
        return user

    def update(self, user: User, auto_commit: bool = True) -> User:
        """
        Persists updates to an existing User entity.
        """
        self.db.add(user)
        if auto_commit:
            self.db.commit()
            self.db.refresh(user)
        else:
            self.db.flush()
        return user

    def soft_delete(self, user_id: str, auto_commit: bool = True) -> bool:
        """
        Performs soft deletion of a user account preserving relational audit trail (AUTH-11).
        Sets `deleted_at` timestamp and marks status as DELETED.
        """
        user = self.get_by_id(user_id, include_deleted=True)
        if not user or user.deleted_at is not None:
            return False

        user.deleted_at = datetime.now(timezone.utc)
        user.status = UserStatus.DELETED
        self.db.add(user)
        if auto_commit:
            self.db.commit()
        else:
            self.db.flush()
        return True

    # ==========================================================================
    # SESSION & TOKEN REVOCATION OPERATIONS (AUTH-10)
    # ==========================================================================

    def create_session(self, session: UserSession, auto_commit: bool = True) -> UserSession:
        """
        Persists an authenticated login session record in `user_sessions`.
        """
        self.db.add(session)
        if auto_commit:
            self.db.commit()
            self.db.refresh(session)
        else:
            self.db.flush()
        return session

    def get_session_by_token_hash(self, refresh_token_hash: str) -> UserSession | None:
        """
        Retrieves a user session by its refresh token hash.
        """
        query = select(UserSession).where(
            UserSession.refresh_token_hash == refresh_token_hash
        )
        return self.db.execute(query).scalar_one_or_none()

    def get_active_session(self, refresh_token_hash: str) -> UserSession | None:
        """
        Retrieves a session ensuring it is NOT revoked and NOT expired (AUTH-10).
        """
        now = datetime.now(timezone.utc)
        query = select(UserSession).where(
            UserSession.refresh_token_hash == refresh_token_hash,
            UserSession.is_revoked.is_(False),
            UserSession.expires_at > now,
        )
        return self.db.execute(query).scalar_one_or_none()

    def revoke_session(self, session_id: str, auto_commit: bool = True) -> bool:
        """
        Marks an individual user session as revoked upon logout (AUTH-10).
        """
        query = select(UserSession).where(UserSession.session_id == session_id)
        session = self.db.execute(query).scalar_one_or_none()
        if not session or session.is_revoked:
            return False

        session.is_revoked = True
        self.db.add(session)
        if auto_commit:
            self.db.commit()
        else:
            self.db.flush()
        return True

    def revoke_all_user_sessions(self, user_id: str, auto_commit: bool = True) -> int:
        """
        Revokes all active sessions for a user (e.g., password change / remote kill).
        Returns the number of sessions revoked.
        """
        query = select(UserSession).where(
            UserSession.user_id == user_id,
            UserSession.is_revoked.is_(False),
        )
        sessions = self.db.execute(query).scalars().all()
        count = 0
        for s in sessions:
            s.is_revoked = True
            self.db.add(s)
            count += 1

        if count > 0:
            if auto_commit:
                self.db.commit()
            else:
                self.db.flush()
        return count

    # ==========================================================================
    # VERIFICATION & OTP OPERATIONS (AUTH-09)
    # ==========================================================================

    def create_verification(self, verification: Verification, auto_commit: bool = True) -> Verification:
        """Persists a newly created verification audit record."""
        self.db.add(verification)
        if auto_commit:
            self.db.commit()
            self.db.refresh(verification)
        else:
            self.db.flush()
        return verification

    def get_latest_pending_verification(
        self, user_id: str, verification_type: VerificationType
    ) -> Verification | None:
        """
        Retrieves the most recent pending verification record for a user and channel.
        """
        query = (
            select(Verification)
            .where(
                Verification.user_id == user_id,
                Verification.type == verification_type,
                Verification.status == VerificationStatus.PENDING,
            )
            .order_by(Verification.expires_at.desc())
        )
        return self.db.execute(query).scalars().first()

    def invalidate_pending_verifications(
        self, user_id: str, verification_type: VerificationType, auto_commit: bool = True
    ) -> int:
        """
        Marks all existing pending verification requests as FAILED before issuing a new one.
        """
        query = select(Verification).where(
            Verification.user_id == user_id,
            Verification.type == verification_type,
            Verification.status == VerificationStatus.PENDING,
        )
        records = self.db.execute(query).scalars().all()
        count = 0
        for r in records:
            r.status = VerificationStatus.FAILED
            self.db.add(r)
            count += 1
        if count > 0:
            if auto_commit:
                self.db.commit()
            else:
                self.db.flush()
        return count

    def update_verification(self, verification: Verification, auto_commit: bool = True) -> Verification:
        """Persists updates to an existing verification record (e.g., attempt counter, status)."""
        self.db.add(verification)
        if auto_commit:
            self.db.commit()
            self.db.refresh(verification)
        else:
            self.db.flush()
        return verification

    def mark_email_verified(self, user_id: str, auto_commit: bool = True) -> bool:
        """Updates user account fast-check flag indicating email confirmation."""
        user = self.get_by_id(user_id)
        if not user:
            return False
        user.is_email_verified = True
        self.db.add(user)
        if auto_commit:
            self.db.commit()
        else:
            self.db.flush()
        return True

    def mark_phone_verified(self, user_id: str, auto_commit: bool = True) -> bool:
        """Updates user account fast-check flag indicating phone confirmation."""
        user = self.get_by_id(user_id)
        if not user:
            return False
        user.is_phone_verified = True
        self.db.add(user)
        if auto_commit:
            self.db.commit()
        else:
            self.db.flush()
        return True
