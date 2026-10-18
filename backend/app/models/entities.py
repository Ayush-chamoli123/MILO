"""
MILO Application Database Entities & Domain Models
===================================================
Production-ready relational database schema and domain entities for the MILO application.
Strictly adheres to:
1. Low-Level Design (LLD) Class Diagram
2. Relational DBDiagram
3. Production Reliability Checklist (Auth, Profiles, Activities, Waitlists, Geo, Chat, Safety)
4. Comprehensive Database Verification Test Cases (AUTH-*, PROF-*, ACT-*, JOIN-*, GEO-*, CHAT-*, NOT-*, SAFE-*, ML-*)
"""

from datetime import date, datetime, time
import enum
from uuid import uuid4

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Column,
    Date as SADate,
    DateTime,
    Enum as SAEnum,
    ForeignKey,
    Index,
    Integer,
    JSON,
    Numeric,
    String,
    Table,
    Text,
    Time as SATime,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base


# ==============================================================================
# HELPER FUNCTIONS
# ==============================================================================

def generate_uuid() -> str:
    """Generates a standard 36-character UUID string for entity primary keys."""
    return str(uuid4())


# ==============================================================================
# ENUM DEFINITIONS (Domain Types)
# ==============================================================================

class UserStatus(str, enum.Enum):
    """Lifecycle status of a user account."""
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    SUSPENDED = "SUSPENDED"
    DELETED = "DELETED"


class ProfileVisibility(str, enum.Enum):
    """Privacy visibility settings for user profiles."""
    PUBLIC = "PUBLIC"
    PRIVATE = "PRIVATE"
    FRIENDS_ONLY = "FRIENDS_ONLY"


class RequirementType(str, enum.Enum):
    """Category classification for user-posted activity requirements."""
    SPORTS = "SPORTS"
    ACTIVITY = "ACTIVITY"
    GROUP = "GROUP"
    OTHER = "OTHER"


class ActivityStatus(str, enum.Enum):
    """
    Operational status of a scheduled activity event.
    Workflow: DRAFT -> OPEN (Active) -> FULL -> COMPLETED (or CANCELLED).
    """
    DRAFT = "DRAFT"
    OPEN = "OPEN"
    FULL = "FULL"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"


class ParticipationStatus(str, enum.Enum):
    """
    Workflow state of a user's participation in an activity.
    Includes WAITLISTED for full-capacity waitlist queuing.
    """
    REQUESTED = "REQUESTED"
    APPROVED = "APPROVED"
    WAITLISTED = "WAITLISTED"
    REJECTED = "REJECTED"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"


class GroupStatus(str, enum.Enum):
    """Lifecycle status of a community group."""
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    CLOSED = "CLOSED"


class MemberRole(str, enum.Enum):
    """Access control role within a group."""
    ADMIN = "ADMIN"
    MEMBER = "MEMBER"


class ConversationType(str, enum.Enum):
    """Chat conversation mode: 1-on-1 direct message or multi-user group chat."""
    DIRECT = "DIRECT"
    GROUP = "GROUP"


class VerificationType(str, enum.Enum):
    """Identity verification channel."""
    EMAIL = "EMAIL"
    PHONE = "PHONE"
    IDENTITY = "IDENTITY"
    PROFILE = "PROFILE"


class VerificationStatus(str, enum.Enum):
    """Review outcome of a user verification request."""
    PENDING = "PENDING"
    VERIFIED = "VERIFIED"
    FAILED = "FAILED"


class NotificationType(str, enum.Enum):
    """System notification event category."""
    MESSAGE = "MESSAGE"
    REQUEST = "REQUEST"
    APPROVAL = "APPROVAL"
    ACTIVITY = "ACTIVITY"
    GROUP = "GROUP"
    SYSTEM = "SYSTEM"


class ReportType(str, enum.Enum):
    """Target entity classification for safety reports."""
    USER = "USER"
    ACTIVITY = "ACTIVITY"
    GROUP = "GROUP"
    MESSAGE = "MESSAGE"
    REVIEW = "REVIEW"


class ReportStatus(str, enum.Enum):
    """Moderation queue review status."""
    OPEN = "OPEN"
    UNDER_REVIEW = "UNDER_REVIEW"
    RESOLVED = "RESOLVED"
    REJECTED = "REJECTED"


class ReviewStatus(str, enum.Enum):
    """Moderation visibility of peer reviews."""
    ACTIVE = "ACTIVE"
    HIDDEN = "HIDDEN"
    DELETED = "DELETED"


class RecommendationType(str, enum.Enum):
    """Classification of AI/matching recommendation items (from LLD MatchingService)."""
    USER = "USER"
    ACTIVITY = "ACTIVITY"
    GROUP = "GROUP"


# ==============================================================================
# ASSOCIATION (JUNCTION) TABLES FOR MANY-TO-MANY RELATIONSHIPS
# ==============================================================================

# User Interests: Maps users to their hobby / interest tags (PROF-04, PROF-05)
user_interests = Table(
    "user_interests",
    Base.metadata,
    Column(
        "user_id",
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing users table",
    ),
    Column(
        "interest_id",
        String(50),
        ForeignKey("interests.interest_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing interests table",
    ),
)

# User Locations: Maps users to their saved / frequent geographic locations
user_locations = Table(
    "user_locations",
    Base.metadata,
    Column(
        "user_id",
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing users table",
    ),
    Column(
        "location_id",
        String(50),
        ForeignKey("locations.location_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing locations table",
    ),
    Column(
        "updated_at",
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
        comment="Timestamp when user location was last refreshed",
    ),
)

# Preferred Activities: Maps matching preferences to desired activity interests
preferred_activities = Table(
    "preferred_activities",
    Base.metadata,
    Column(
        "preference_id",
        String(50),
        ForeignKey("preferences.preference_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing preferences table",
    ),
    Column(
        "interest_id",
        String(50),
        ForeignKey("interests.interest_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing interests table",
    ),
)

# Activity Interests: Associates activity events with descriptive interests
activity_interests = Table(
    "activity_interests",
    Base.metadata,
    Column(
        "activity_id",
        String(50),
        ForeignKey("activities.activity_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing activities table",
    ),
    Column(
        "interest_id",
        String(50),
        ForeignKey("interests.interest_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing interests table",
    ),
)

# Conversation Participants: Maps chat conversations to member users with read-state (CHAT-09)
conversation_participants = Table(
    "conversation_participants",
    Base.metadata,
    Column(
        "conversation_id",
        String(50),
        ForeignKey("conversations.conversation_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing conversations table",
    ),
    Column(
        "user_id",
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        primary_key=True,
        comment="Foreign key referencing users table",
    ),
    Column(
        "joined_at",
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when user joined the conversation",
    ),
    Column(
        "last_read_at",
        DateTime(timezone=True),
        nullable=True,
        comment="Timestamp of user's last read receipt for instant unread badge counts",
    ),
    Column(
        "last_read_message_id",
        String(50),
        nullable=True,
        comment="ID of the last message acknowledged by user",
    ),
    Column(
        "is_muted",
        Boolean,
        default=False,
        nullable=False,
        comment="Whether the user has muted push notifications for this conversation",
    ),
)


# ==============================================================================
# CORE DOMAIN ENTITY MODELS
# ==============================================================================

class User(Base):
    """
    User Entity: Central account model for authentication, identity, and security.
    Validates AUTH-01 through AUTH-12: email uniqueness, phone uniqueness, email format check,
    soft deletion, and fast verification status.
    """
    __tablename__ = "users"
    __table_args__ = (
        CheckConstraint(
            "email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'",
            name="ck_users_email_format",
        ),
        Index("ix_users_status_deleted", "status", "deleted_at"),
    )

    user_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the user (UUID)",
    )
    full_name: Mapped[str] = mapped_column(
        String(150),
        nullable=False,
        comment="Full legal or display name of the user",
    )
    username: Mapped[str] = mapped_column(
        String(100),
        unique=True,
        index=True,
        nullable=False,
        comment="Unique handle for user mention and profile URLs",
    )
    email: Mapped[str] = mapped_column(
        String(150),
        unique=True,
        index=True,
        nullable=False,
        comment="Unique normalized email address used for login and notifications",
    )
    phone_number: Mapped[str | None] = mapped_column(
        String(20),
        unique=True,
        index=True,
        nullable=True,
        comment="Optional unique phone number for SMS and phone OTP verification (AUTH-03)",
    )
    password_hash: Mapped[str] = mapped_column(
        String(255),
        nullable=False,
        comment="Securely salted and hashed password (AUTH-08)",
    )
    profile_picture: Mapped[str | None] = mapped_column(
        String(500),
        nullable=True,
        comment="URL pointing to the user's avatar image (PROF-03, PROF-07)",
    )
    age: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Age in years for activity filtering and preference matching",
    )
    gender: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Self-identified gender string",
    )
    city: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Primary residential city",
    )
    languages: Mapped[list | None] = mapped_column(
        JSON,
        nullable=True,
        comment="List of spoken languages (from LLD List<String>)",
    )
    profile_visibility: Mapped[ProfileVisibility] = mapped_column(
        SAEnum(ProfileVisibility, name="profile_visibility", native_enum=True),
        default=ProfileVisibility.PUBLIC,
        nullable=False,
        comment="User-level privacy visibility preference (PROF-06)",
    )
    status: Mapped[UserStatus] = mapped_column(
        SAEnum(UserStatus, name="user_status", native_enum=True),
        default=UserStatus.ACTIVE,
        nullable=False,
        comment="Account state (ACTIVE, INACTIVE, SUSPENDED, DELETED)",
    )
    is_email_verified: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Direct fast-check boolean indicating email confirmation",
    )
    is_phone_verified: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Direct fast-check boolean indicating phone confirmation",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Account creation timestamp (timestamptz)",
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Soft-deletion timestamp preserving relational audit trail (AUTH-11)",
    )

    # 1:1 Relationships
    profile: Mapped["Profile | None"] = relationship(
        back_populates="user",
        uselist=False,
        cascade="all, delete-orphan",
    )
    preference: Mapped["Preference | None"] = relationship(
        back_populates="user",
        uselist=False,
        cascade="all, delete-orphan",
    )

    # 1:Many Auth Sessions (AUTH-10)
    sessions: Mapped[list["UserSession"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )

    # Many-to-Many Relationships
    interests: Mapped[list["Interest"]] = relationship(
        secondary=user_interests,
        back_populates="users",
    )
    locations: Mapped[list["Location"]] = relationship(
        secondary=user_locations,
        back_populates="users",
    )

    # 1:Many Domain Relationships
    created_requirements: Mapped[list["Requirement"]] = relationship(
        back_populates="creator",
        cascade="all, delete-orphan",
    )
    hosted_activities: Mapped[list["Activity"]] = relationship(
        back_populates="host",
        cascade="all, delete-orphan",
    )
    participations: Mapped[list["Participation"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    created_groups: Mapped[list["Group"]] = relationship(
        back_populates="creator",
        cascade="all, delete-orphan",
    )
    group_memberships: Mapped[list["GroupMember"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    conversations: Mapped[list["Conversation"]] = relationship(
        secondary=conversation_participants,
        back_populates="participants",
    )
    sent_messages: Mapped[list["Message"]] = relationship(
        back_populates="sender",
        cascade="all, delete-orphan",
    )
    notifications: Mapped[list["Notification"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    verifications: Mapped[list["Verification"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    reports_submitted: Mapped[list["Report"]] = relationship(
        foreign_keys="[Report.reporter_id]",
        back_populates="reporter",
        cascade="all, delete-orphan",
    )
    reports_received: Mapped[list["Report"]] = relationship(
        foreign_keys="[Report.reported_user_id]",
        back_populates="reported_user",
    )
    reviews_written: Mapped[list["Review"]] = relationship(
        foreign_keys="[Review.reviewer_id]",
        back_populates="reviewer",
        cascade="all, delete-orphan",
    )
    reviews_received: Mapped[list["Review"]] = relationship(
        foreign_keys="[Review.reviewed_user_id]",
        back_populates="reviewed_user",
    )
    ratings_given: Mapped[list["Rating"]] = relationship(
        foreign_keys="[Rating.reviewer_id]",
        back_populates="reviewer",
        cascade="all, delete-orphan",
    )
    ratings_received: Mapped[list["Rating"]] = relationship(
        foreign_keys="[Rating.rated_user_id]",
        back_populates="rated_user",
    )
    blocks_initiated: Mapped[list["Block"]] = relationship(
        foreign_keys="[Block.blocker_id]",
        back_populates="blocker",
        cascade="all, delete-orphan",
    )
    blocks_received: Mapped[list["Block"]] = relationship(
        foreign_keys="[Block.blocked_user_id]",
        back_populates="blocked_user",
    )
    recommendations: Mapped[list["Recommendation"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    audit_logs: Mapped[list["AuditLog"]] = relationship(
        back_populates="user",
    )
    consents: Mapped[list["UserConsent"]] = relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )


class UserSession(Base):
    """
    User Session Entity: Manages active login sessions, device metadata,
    token expiry, and immediate revocation upon logout (AUTH-10).
    """
    __tablename__ = "user_sessions"
    __table_args__ = (
        Index("ix_user_sessions_user_revoked", "user_id", "is_revoked"),
    )

    session_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for login session record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing authenticated user",
    )
    refresh_token_hash: Mapped[str] = mapped_column(
        String(255),
        unique=True,
        nullable=False,
        comment="Cryptographic hash of the active refresh token",
    )
    device_name: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Client device descriptor (e.g., Pixel 8 Pro, Chrome / Windows 11)",
    )
    ip_address: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Client network IP address at session inception",
    )
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        comment="Session / refresh token expiration timestamp",
    )
    is_revoked: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Revocation flag triggered on user logout or remote session kill",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when session was issued",
    )

    user: Mapped["User"] = relationship(back_populates="sessions")


class Profile(Base):
    """
    Profile Entity: Extended user persona, public biography, skills, and reputation.
    Guarantees exactly one profile per user via unique=True on user_id (PROF-01, PROF-02).
    """
    __tablename__ = "profiles"

    profile_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the profile record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
        comment="Foreign key referencing the associated user account (1:1 constraint)",
    )
    bio: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Descriptive self-summary or bio (PROF-03)",
    )
    skills: Mapped[list | None] = mapped_column(
        JSON,
        nullable=True,
        comment="User skills and competencies (e.g., Badminton Coach, Guitar, French)",
    )
    profile_visibility: Mapped[ProfileVisibility] = mapped_column(
        SAEnum(ProfileVisibility, name="profile_visibility", native_enum=True),
        default=ProfileVisibility.PUBLIC,
        nullable=False,
        comment="Granular visibility control for profile details (PROF-06)",
    )
    rating: Mapped[float] = mapped_column(
        Numeric(3, 2),
        default=0.00,
        nullable=False,
        comment="Aggregated community star rating (0.00 to 5.00)",
    )
    reputation: Mapped[str] = mapped_column(
        String(100),
        default="New Member",
        nullable=False,
        comment="Community standing or trust badge level",
    )

    user: Mapped["User"] = relationship(back_populates="profile")


class Interest(Base):
    """
    Interest Entity: Reusable taxonomy of activities, sports, and hobbies.
    Foreign keys protect against unknown interest IDs (PROF-05).
    """
    __tablename__ = "interests"

    interest_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the interest",
    )
    name: Mapped[str] = mapped_column(
        String(100),
        unique=True,
        nullable=False,
        comment="Interest name (e.g., Badminton, Chess, Coding)",
    )
    category: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Higher-level categorization (e.g., Sports, Tech, Arts)",
    )

    users: Mapped[list["User"]] = relationship(
        secondary=user_interests,
        back_populates="interests",
    )
    preferences: Mapped[list["Preference"]] = relationship(
        secondary=preferred_activities,
        back_populates="preferred_activities",
    )
    activities: Mapped[list["Activity"]] = relationship(
        secondary=activity_interests,
        back_populates="interests",
    )


class Preference(Base):
    """
    Preference Entity: Matchmaking, privacy, and notification settings configured by user (NOT-05).
    """
    __tablename__ = "preferences"

    preference_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for user matching preference record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
        comment="Foreign key referencing the user owner",
    )
    radius: Mapped[float | None] = mapped_column(
        Numeric(10, 2),
        nullable=True,
        comment="Maximum discovery search radius in kilometers (GEO-01, GEO-02)",
    )
    preferred_group_size: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Desired maximum or optimal group participant size",
    )
    show_location: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Privacy toggle controlling whether approximate location is visible to nearby users (GEO-11)",
    )
    show_activity_history: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Privacy toggle controlling whether past activity history is public",
    )
    email_notifications: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Notification toggle for email delivery",
    )
    push_notifications: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Notification toggle for mobile/web push delivery",
    )
    activity_alerts: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Notification toggle for nearby activity recommendations and updates",
    )
    chat_alerts: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="Notification toggle for new direct and group chat messages",
    )

    user: Mapped["User"] = relationship(back_populates="preference")
    preferred_activities: Mapped[list["Interest"]] = relationship(
        secondary=preferred_activities,
        back_populates="preferences",
    )


class Location(Base):
    """
    Location Entity: Geographic coordinates, place names, and privacy layers.
    Includes coordinate validity check constraints (GEO-04: latitude [-90, 90], longitude [-180, 180]).
    Dual representation: fuzzed public approximate_location vs exact_location (GEO-11).
    """
    __tablename__ = "locations"
    __table_args__ = (
        CheckConstraint(
            "latitude >= -90.0 AND latitude <= 90.0",
            name="ck_locations_latitude_valid",
        ),
        CheckConstraint(
            "longitude >= -180.0 AND longitude <= 180.0",
            name="ck_locations_longitude_valid",
        ),
        Index("ix_locations_lat_lon", "latitude", "longitude"),
        Index("ix_locations_city_country", "city", "country"),
    )

    location_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for location entry",
    )
    latitude: Mapped[float] = mapped_column(
        Numeric(10, 7),
        nullable=False,
        comment="Geographic latitude coordinate with high precision",
    )
    longitude: Mapped[float] = mapped_column(
        Numeric(10, 7),
        nullable=False,
        comment="Geographic longitude coordinate with high precision",
    )
    location_name: Mapped[str | None] = mapped_column(
        String(150),
        nullable=True,
        comment="Name of venue, ground, landmark, or park",
    )
    approximate_location: Mapped[str | None] = mapped_column(
        String(255),
        nullable=True,
        comment="Fuzzed or neighborhood-level location shown to non-connections",
    )
    exact_location: Mapped[str | None] = mapped_column(
        String(255),
        nullable=True,
        comment="Precise address or venue details disclosed upon approval",
    )
    city: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="City or municipality name for geographic search and filtering",
    )
    state: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="State or province name",
    )
    country: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Country name or ISO code",
    )
    postal_code: Mapped[str | None] = mapped_column(
        String(20),
        nullable=True,
        comment="Postal or ZIP code",
    )

    users: Mapped[list["User"]] = relationship(
        secondary=user_locations,
        back_populates="locations",
    )
    activities: Mapped[list["Activity"]] = relationship(
        back_populates="location",
    )


class Requirement(Base):
    """
    Requirement Entity: User-created request for people to engage in an activity or sport.
    A requirement is the root post that may evolve into or link to a scheduled Activity.
    """
    __tablename__ = "requirements"
    __table_args__ = (
        Index("ix_requirements_type_status", "type", "status"),
    )

    requirement_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the requirement post",
    )
    creator_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing the user who posted the requirement",
    )
    title: Mapped[str] = mapped_column(
        String(200),
        nullable=False,
        comment="Concise headline of the requirement (e.g., Need 2 players for Sunday Football)",
    )
    type: Mapped[RequirementType] = mapped_column(
        SAEnum(RequirementType, name="requirement_type", native_enum=True),
        default=RequirementType.ACTIVITY,
        nullable=False,
        comment="Category type (SPORTS, ACTIVITY, GROUP, OTHER)",
    )
    category: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Descriptive sub-category or genre",
    )
    date: Mapped[date | None] = mapped_column(
        SADate,
        nullable=True,
        comment="Target date for the planned requirement",
    )
    time: Mapped[time | None] = mapped_column(
        SATime,
        nullable=True,
        comment="Target time of day for the requirement",
    )
    date_flexible: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Flag indicating if the organizer can adjust the date",
    )
    time_flexible: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Flag indicating if the organizer can adjust the time",
    )
    description: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Comprehensive narrative of the activity requirement and expectations",
    )
    people_needed: Mapped[int] = mapped_column(
        Integer,
        default=1,
        nullable=False,
        comment="Total number of companion participants sought",
    )
    skill_level: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Preferred experience level (e.g., Beginner, Intermediate, Pro)",
    )
    status: Mapped[str] = mapped_column(
        String(50),
        default="OPEN",
        nullable=False,
        comment="Status of the requirement (OPEN, CLOSED, FULFILLED)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when requirement was published",
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Soft-deletion timestamp preserving historical audit data",
    )

    creator: Mapped["User"] = relationship(back_populates="created_requirements")
    activities: Mapped[list["Activity"]] = relationship(
        back_populates="requirement",
    )


class Activity(Base):
    """
    Activity Entity: A concrete scheduled event with host, location, participant capacity,
    eligibility constraints, optimistic concurrency lock, and soft deletion.
    Enforces ACT-01 to ACT-12 and JOIN-01 to JOIN-15 with strict DB check constraints:
    - Positive capacity (ACT-03)
    - Valid start/end time order (ACT-04)
    - Capacity cannot be exceeded at database level (JOIN-11 concurrency safeguard)
    """
    __tablename__ = "activities"
    __table_args__ = (
        CheckConstraint(
            "max_participants IS NULL OR max_participants > 0",
            name="ck_activities_max_participants_positive",
        ),
        CheckConstraint(
            "current_participants_count >= 0",
            name="ck_activities_current_participants_non_negative",
        ),
        CheckConstraint(
            "current_participants_count <= max_participants OR max_participants IS NULL",
            name="ck_activities_capacity_not_exceeded",
        ),
        CheckConstraint(
            "end_time IS NULL OR time IS NULL OR end_time > time",
            name="ck_activities_time_order",
        ),
        Index("ix_activities_host_id", "host_id"),
        Index("ix_activities_date_time", "date", "time"),
        Index("ix_activities_status_date", "status", "date"),
    )

    activity_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the activity event",
    )
    requirement_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("requirements.requirement_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional foreign key linking activity back to originating requirement",
    )
    host_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing the user hosting the activity (ACT-02)",
    )
    title: Mapped[str] = mapped_column(
        String(200),
        nullable=False,
        comment="Title or name of the activity (ACT-02: NOT NULL)",
    )
    description: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Detailed plan, agenda, or guidelines for participants",
    )
    category: Mapped[str] = mapped_column(
        String(100),
        default="General",
        nullable=False,
        comment="Activity category (Sports, Study, Walking, Cycling, Social)",
    )
    date: Mapped[date | None] = mapped_column(
        SADate,
        nullable=True,
        comment="Date on which the activity is scheduled to take place",
    )
    time: Mapped[time | None] = mapped_column(
        SATime,
        nullable=True,
        comment="Time at which the activity begins",
    )
    end_time: Mapped[time | None] = mapped_column(
        SATime,
        nullable=True,
        comment="Time at which the activity concludes (ACT-04)",
    )
    max_participants: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Upper cap on participant approvals (ACT-03: must be > 0)",
    )
    current_participants_count: Mapped[int] = mapped_column(
        Integer,
        default=0,
        nullable=False,
        comment="Fast-access counter for approved participants to enforce capacity",
    )
    status: Mapped[ActivityStatus] = mapped_column(
        SAEnum(ActivityStatus, name="activity_status", native_enum=True),
        default=ActivityStatus.OPEN,
        nullable=False,
        comment="Current activity state (DRAFT, OPEN, FULL, CANCELLED, COMPLETED)",
    )
    location_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("locations.location_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Foreign key to the physical venue/location record",
    )

    # Participant Eligibility Gates
    min_age: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Minimum age requirement for participation eligibility",
    )
    max_age: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Maximum age threshold for participation eligibility",
    )
    gender_restriction: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Gender eligibility rule (e.g., ALL, WOMEN_ONLY, MEN_ONLY)",
    )
    required_skill_level: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Minimum skill competency required (e.g., All Levels, Intermediate+)",
    )

    # Concurrency & Soft-Deletion
    version_id: Mapped[int] = mapped_column(
        Integer,
        default=1,
        nullable=False,
        comment="Optimistic locking version counter preventing race-condition overbooking",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when activity was created",
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Soft-deletion timestamp preserving participation and review history (ACT-10)",
    )

    host: Mapped["User"] = relationship(back_populates="hosted_activities")
    requirement: Mapped["Requirement | None"] = relationship(back_populates="activities")
    location: Mapped["Location | None"] = relationship(back_populates="activities")
    interests: Mapped[list["Interest"]] = relationship(
        secondary=activity_interests,
        back_populates="activities",
    )
    participations: Mapped[list["Participation"]] = relationship(
        back_populates="activity",
        cascade="all, delete-orphan",
    )
    reviews: Mapped[list["Review"]] = relationship(
        back_populates="activity",
    )
    ratings: Mapped[list["Rating"]] = relationship(
        back_populates="activity",
    )
    conversations: Mapped[list["Conversation"]] = relationship(
        back_populates="activity",
    )


class Participation(Base):
    """
    Participation Entity: Tracks user join requests, approvals, waitlists, and attendance.
    Enforces JOIN-01 through JOIN-15:
    - Unique (activity_id, user_id) stops duplicate active membership (JOIN-03, JOIN-12).
    - Status transitions: REQUESTED -> APPROVED / WAITLISTED / REJECTED / CANCELLED.
    """
    __tablename__ = "participations"
    __table_args__ = (
        UniqueConstraint(
            "activity_id", "user_id", name="uq_participation_activity_user"
        ),
        Index("ix_participations_user_activity", "user_id", "activity_id"),
        Index("ix_participations_activity_waitlist", "activity_id", "waitlist_position"),
    )

    participation_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for participation record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing participant user",
    )
    activity_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing target activity",
    )
    role: Mapped[str] = mapped_column(
        String(50),
        default="MEMBER",
        nullable=False,
        comment="Participant role in activity (e.g., HOST, CO_HOST, MEMBER)",
    )
    status: Mapped[ParticipationStatus] = mapped_column(
        SAEnum(ParticipationStatus, name="participation_status", native_enum=True),
        default=ParticipationStatus.REQUESTED,
        nullable=False,
        comment="Workflow status (REQUESTED, APPROVED, WAITLISTED, REJECTED, CANCELLED, COMPLETED)",
    )
    waitlist_position: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Priority queue index when activity is at full capacity (JOIN-02, JOIN-14)",
    )
    request_date: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when user submitted request to join (JOIN-06)",
    )
    approved_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Timestamp when host approved the participation request (JOIN-07)",
    )
    joined_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Timestamp when participant checked in or joined",
    )

    user: Mapped["User"] = relationship(back_populates="participations")
    activity: Mapped["Activity"] = relationship(back_populates="participations")


class Group(Base):
    """
    Group Entity: Ongoing social or sports club formed by users.
    Includes soft deletion and activity/chat associations.
    """
    __tablename__ = "groups"

    group_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the group",
    )
    name: Mapped[str] = mapped_column(
        String(150),
        nullable=False,
        comment="Public name of the group",
    )
    description: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Description of the group's purpose, rules, and focus",
    )
    creator_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing the founding user",
    )
    status: Mapped[GroupStatus] = mapped_column(
        SAEnum(GroupStatus, name="group_status", native_enum=True),
        default=GroupStatus.ACTIVE,
        nullable=False,
        comment="Group lifecycle status (ACTIVE, INACTIVE, CLOSED)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when group was created",
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Soft-deletion timestamp for historical membership audits",
    )

    creator: Mapped["User"] = relationship(back_populates="created_groups")
    members: Mapped[list["GroupMember"]] = relationship(
        back_populates="group",
        cascade="all, delete-orphan",
    )
    conversations: Mapped[list["Conversation"]] = relationship(
        back_populates="group",
    )


class GroupMember(Base):
    """
    Group Member Entity: Membership record assigning member roles in a group.
    """
    __tablename__ = "group_members"
    __table_args__ = (
        UniqueConstraint("group_id", "user_id", name="uq_group_member_user"),
        Index("ix_group_members_group_user", "group_id", "user_id"),
    )

    group_member_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for group membership record",
    )
    group_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("groups.group_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing parent group",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing member user",
    )
    role: Mapped[MemberRole] = mapped_column(
        SAEnum(MemberRole, name="member_role", native_enum=True),
        default=MemberRole.MEMBER,
        nullable=False,
        comment="Role privileges in the group (ADMIN or MEMBER)",
    )
    joined_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when user joined group",
    )

    group: Mapped["Group"] = relationship(back_populates="members")
    user: Mapped["User"] = relationship(back_populates="group_memberships")


class Conversation(Base):
    """
    Conversation Entity: Message thread container (Direct or Group chat).
    Can be explicitly attached to an Activity or a Community Group (CHAT-01, CHAT-10).
    """
    __tablename__ = "conversations"

    conversation_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for conversation thread",
    )
    type: Mapped[ConversationType] = mapped_column(
        SAEnum(ConversationType, name="conversation_type", native_enum=True),
        default=ConversationType.DIRECT,
        nullable=False,
        comment="Channel format: DIRECT (1-on-1) or GROUP",
    )
    activity_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional foreign key binding this conversation directly to an Activity chat",
    )
    group_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("groups.group_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional foreign key binding this conversation directly to a Group chat",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when conversation was initiated",
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
        comment="Timestamp of the most recent message in this conversation",
    )

    participants: Mapped[list["User"]] = relationship(
        secondary=conversation_participants,
        back_populates="conversations",
    )
    messages: Mapped[list["Message"]] = relationship(
        back_populates="conversation",
        cascade="all, delete-orphan",
    )
    activity: Mapped["Activity | None"] = relationship(back_populates="conversations")
    group: Mapped["Group | None"] = relationship(back_populates="conversations")


class Message(Base):
    """
    Message Entity: Real-time chat message sent inside a conversation.
    Enforces CHAT-01 to CHAT-12:
    - Empty messages rejected by check constraint (CHAT-03)
    - Excessively long messages limited by length check (CHAT-04)
    - Retry idempotency prevented by client idempotency_key index (CHAT-08)
    - Chronological ordering supported by (conversation_id, timestamp) index (CHAT-07)
    """
    __tablename__ = "messages"
    __table_args__ = (
        CheckConstraint(
            "length(trim(content)) > 0",
            name="ck_messages_content_not_empty",
        ),
        CheckConstraint(
            "length(content) <= 5000",
            name="ck_messages_content_max_len",
        ),
        Index("ix_messages_conv_timestamp", "conversation_id", "timestamp"),
        Index("ix_messages_idempotency", "conversation_id", "idempotency_key"),
    )

    message_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the message",
    )
    conversation_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("conversations.conversation_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing parent conversation thread",
    )
    sender_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing sending user",
    )
    content: Mapped[str] = mapped_column(
        Text,
        nullable=False,
        comment="Message body text content (CHAT-03: non-empty, CHAT-04: <= 5000 chars)",
    )
    message_type: Mapped[str] = mapped_column(
        String(30),
        default="TEXT",
        nullable=False,
        comment="Payload format: TEXT, IMAGE, SYSTEM_EVENT, LOCATION_PIN",
    )
    media_url: Mapped[str | None] = mapped_column(
        String(500),
        nullable=True,
        comment="Optional URL to attached media file or image",
    )
    idempotency_key: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Client GUID preventing duplicate messages during network retries (CHAT-08)",
    )
    timestamp: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when message was dispatched",
    )
    status: Mapped[str] = mapped_column(
        String(50),
        default="SENT",
        nullable=False,
        comment="Delivery receipt status (e.g., SENT, DELIVERED, READ)",
    )

    conversation: Mapped["Conversation"] = relationship(back_populates="messages")
    sender: Mapped["User"] = relationship(back_populates="sent_messages")


class Notification(Base):
    """
    Notification Entity: Push alerts and activity updates sent to users (NOT-01 to NOT-08).
    """
    __tablename__ = "notifications"
    __table_args__ = (
        Index("ix_notifications_user_read", "user_id", "is_read"),
    )

    notification_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for notification",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing recipient user (NOT-01)",
    )
    type: Mapped[NotificationType] = mapped_column(
        SAEnum(NotificationType, name="notification_type", native_enum=True),
        nullable=False,
        comment="Notification classification (MESSAGE, REQUEST, APPROVAL, ACTIVITY, etc.)",
    )
    message: Mapped[str] = mapped_column(
        Text,
        nullable=False,
        comment="Human-readable notification text payload",
    )
    reference_id: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Polymorphic ID of the target object (activity_id, message_id, etc. NOT-08)",
    )
    is_read: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Flag indicating if notification was acknowledged by recipient (NOT-02, NOT-03)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when notification was generated",
    )

    user: Mapped["User"] = relationship(back_populates="notifications")


class OutboxEvent(Base):
    """
    Transactional Outbox Table: Guarantees reliable, at-least-once event delivery for notifications,
    webhooks, and async workers without duplicate broadcasts (NOT-04, NOT-06).
    """
    __tablename__ = "outbox_events"
    __table_args__ = (
        Index("ix_outbox_status_created", "status", "created_at"),
    )

    outbox_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for outbox event",
    )
    event_type: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
        comment="Event identifier (e.g. ACTIVITY_JOIN_REQUESTED, ACTIVITY_CANCELLED)",
    )
    aggregate_type: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="Originating entity type (e.g. ACTIVITY, USER, PARTICIPATION)",
    )
    aggregate_id: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="ID of the originating entity",
    )
    payload: Mapped[dict] = mapped_column(
        JSON,
        nullable=False,
        comment="Complete JSON serializable event payload for worker delivery",
    )
    status: Mapped[str] = mapped_column(
        String(20),
        default="PENDING",
        nullable=False,
        comment="Dispatch status: PENDING, PROCESSED, FAILED",
    )
    retry_count: Mapped[int] = mapped_column(
        Integer,
        default=0,
        nullable=False,
        comment="Count of delivery attempts",
    )
    error_message: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Last recorded failure reason",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when event was written in transaction",
    )
    processed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Timestamp when worker completed event delivery",
    )


class Report(Base):
    """
    Report Entity: Trust & Safety moderation flags filed against users, activities, or content (SAFE-08, SAFE-09).
    """
    __tablename__ = "reports"

    report_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for report ticket",
    )
    reporter_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing reporting user",
    )
    reported_user_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional foreign key referencing reported user account",
    )
    target_id: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="ID of reported entity (activity_id, group_id, message_id, review_id)",
    )
    type: Mapped[ReportType] = mapped_column(
        SAEnum(ReportType, name="report_type", native_enum=True),
        nullable=False,
        comment="Entity type under review (USER, ACTIVITY, GROUP, MESSAGE, REVIEW)",
    )
    reason: Mapped[str] = mapped_column(
        String(255),
        nullable=False,
        comment="Short violation category or rule broken",
    )
    description: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Detailed incident report and context provided by reporter",
    )
    status: Mapped[ReportStatus] = mapped_column(
        SAEnum(ReportStatus, name="report_status", native_enum=True),
        default=ReportStatus.OPEN,
        nullable=False,
        comment="Investigation status (OPEN, UNDER_REVIEW, RESOLVED, REJECTED)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when report was submitted",
    )

    reporter: Mapped["User"] = relationship(
        foreign_keys=[reporter_id],
        back_populates="reports_submitted",
    )
    reported_user: Mapped["User | None"] = relationship(
        foreign_keys=[reported_user_id],
        back_populates="reports_received",
    )


class Verification(Base):
    """
    Verification Entity: Identity verification codes, OTP tokens, and audit status (AUTH-09).
    Supports email links, SMS OTPs, document uploads, and attempt throttling.
    """
    __tablename__ = "verifications"
    __table_args__ = (
        Index("ix_verifications_user_type_status", "user_id", "type", "status"),
    )

    verification_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for verification audit record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing the user undergoing verification",
    )
    type: Mapped[VerificationType] = mapped_column(
        SAEnum(VerificationType, name="verification_type", native_enum=True),
        nullable=False,
        comment="Verification type (EMAIL, PHONE, IDENTITY, PROFILE)",
    )
    status: Mapped[VerificationStatus] = mapped_column(
        SAEnum(VerificationStatus, name="verification_status", native_enum=True),
        default=VerificationStatus.PENDING,
        nullable=False,
        comment="Current review status (PENDING, VERIFIED, FAILED)",
    )
    token_hash: Mapped[str | None] = mapped_column(
        String(255),
        nullable=True,
        comment="Hashed secure token for email verification links or password resets",
    )
    otp_code: Mapped[str | None] = mapped_column(
        String(10),
        nullable=True,
        comment="Short numeric OTP code for SMS or email 2FA verification",
    )
    attempts: Mapped[int] = mapped_column(
        Integer,
        default=0,
        nullable=False,
        comment="Verification attempt counter for rate limiting brute-force attempts",
    )
    expires_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Expiration timestamp for OTP or verification link (AUTH-09)",
    )
    verified_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        comment="Timestamp when verification was officially confirmed",
    )

    user: Mapped["User"] = relationship(back_populates="verifications")


class Review(Base):
    """
    Review Entity: Qualitative written feedback left after completing an activity (SAFE-01 to SAFE-05).
    Guarantees one review per reviewer per activity via unique constraint (SAFE-03).
    """
    __tablename__ = "reviews"
    __table_args__ = (
        UniqueConstraint("activity_id", "reviewer_id", name="uq_review_activity_reviewer"),
    )

    review_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for review entry",
    )
    activity_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional foreign key referencing related activity",
    )
    reviewer_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing author user",
    )
    reviewed_user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing evaluated user",
    )
    content: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        comment="Written review critique and testimonial",
    )
    status: Mapped[ReviewStatus] = mapped_column(
        SAEnum(ReviewStatus, name="review_status", native_enum=True),
        default=ReviewStatus.ACTIVE,
        nullable=False,
        comment="Visibility state of review (ACTIVE, HIDDEN, DELETED)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when review was posted",
    )

    activity: Mapped["Activity | None"] = relationship(back_populates="reviews")
    reviewer: Mapped["User"] = relationship(
        foreign_keys=[reviewer_id],
        back_populates="reviews_written",
    )
    reviewed_user: Mapped["User"] = relationship(
        foreign_keys=[reviewed_user_id],
        back_populates="reviews_received",
    )


class Rating(Base):
    """
    Rating Entity: Quantitative 1-to-5 star evaluation left for peers after activities (SAFE-02).
    Enforces 1 to 5 numeric check constraint and prevents duplicate ratings.
    """
    __tablename__ = "ratings"
    __table_args__ = (
        CheckConstraint("score >= 1 AND score <= 5", name="ck_rating_score_range"),
        UniqueConstraint(
            "reviewer_id", "rated_user_id", "activity_id",
            name="uq_rating_reviewer_rated_activity",
        ),
    )

    rating_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for rating score entry",
    )
    activity_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Foreign key to activity where users interacted",
    )
    reviewer_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing evaluator user",
    )
    rated_user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing user receiving score",
    )
    score: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        comment="Rating value between 1 (poor) and 5 (excellent) - SAFE-02",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when score was awarded",
    )

    activity: Mapped["Activity | None"] = relationship(back_populates="ratings")
    reviewer: Mapped["User"] = relationship(
        foreign_keys=[reviewer_id],
        back_populates="ratings_given",
    )
    rated_user: Mapped["User"] = relationship(
        foreign_keys=[rated_user_id],
        back_populates="ratings_received",
    )


class Block(Base):
    """
    Block Entity: Restricts interpersonal communication and visibility between users (SAFE-06, SAFE-07).
    Prevents self-blocking and duplicate block pairs.
    """
    __tablename__ = "blocks"
    __table_args__ = (
        CheckConstraint("blocker_id != blocked_user_id", name="ck_block_not_self"),
        UniqueConstraint("blocker_id", "blocked_user_id", name="uq_user_block"),
    )

    block_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for user block record",
    )
    blocker_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing blocking user",
    )
    blocked_user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing user who has been blocked",
    )
    reason: Mapped[str | None] = mapped_column(
        String(255),
        nullable=True,
        comment="Optional reason provided by user for placing block",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when block was instituted",
    )

    blocker: Mapped["User"] = relationship(
        foreign_keys=[blocker_id],
        back_populates="blocks_initiated",
    )
    blocked_user: Mapped["User"] = relationship(
        foreign_keys=[blocked_user_id],
        back_populates="blocks_received",
    )


class Recommendation(Base):
    """
    Recommendation Entity: Ranked match suggestions computed by the Matching Service (ML-04, ML-09, ML-11).
    Includes machine learning model versioning and user interaction event tracking.
    """
    __tablename__ = "recommendations"
    __table_args__ = (
        Index("ix_recommendations_user_score", "user_id", "score"),
        Index("ix_recommendations_model_event", "model_version", "event_status"),
    )

    recommendation_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for recommendation entry",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        index=True,
        comment="Foreign key referencing target user who receives suggestion",
    )
    target_id: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="Polymorphic ID of suggested user, activity, or group",
    )
    type: Mapped[RecommendationType] = mapped_column(
        SAEnum(RecommendationType, name="recommendation_type", native_enum=True),
        nullable=False,
        comment="Type of suggested entity (USER, ACTIVITY, GROUP)",
    )
    score: Mapped[float] = mapped_column(
        Numeric(5, 4),
        default=0.0000,
        nullable=False,
        comment="Match compatibility score between 0.0000 and 1.0000",
    )
    model_version: Mapped[str] = mapped_column(
        String(50),
        default="v1.0",
        nullable=False,
        comment="ML model algorithm or pipeline version tag (ML-09: e.g. v1.2-two-tower)",
    )
    event_status: Mapped[str] = mapped_column(
        String(30),
        default="SERVED",
        nullable=False,
        comment="User interaction state: SERVED, VIEWED, CLICKED, ACCEPTED, DISMISSED (ML-11)",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when recommendation was generated",
    )

    user: Mapped["User"] = relationship(back_populates="recommendations")
    feedback_entries: Mapped[list["RecommendationFeedback"]] = relationship(
        back_populates="recommendation",
        cascade="all, delete-orphan",
    )


class AuditLog(Base):
    """
    AuditLog Entity: Security, regulatory compliance, and operations tracking.
    Captures sensitive actions (account deletion, login attempts, rate limit warnings,
    activity cancellations, and moderation decisions).
    """
    __tablename__ = "audit_logs"
    __table_args__ = (
        Index("ix_audit_logs_action_created", "action", "created_at"),
        Index("ix_audit_logs_entity", "entity_type", "entity_id"),
    )

    audit_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for audit log entry",
    )
    user_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="SET NULL"),
        nullable=True,
        index=True,
        comment="Optional actor user ID who initiated the event",
    )
    action: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
        comment="Action name (e.g. USER_LOGIN, PASSWORD_RESET, ACCOUNT_DELETED, CANCEL_ACTIVITY)",
    )
    entity_type: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="Affected domain entity type (e.g. USER, ACTIVITY, PARTICIPATION, REPORT)",
    )
    entity_id: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="ID of the affected entity",
    )
    details: Mapped[dict | None] = mapped_column(
        JSON,
        nullable=True,
        comment="Arbitrary contextual metadata, request headers, or parameter diffs",
    )
    ip_address: Mapped[str | None] = mapped_column(
        String(50),
        nullable=True,
        comment="Client IP address recorded at the time of action",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when event occurred",
    )

    user: Mapped["User | None"] = relationship(back_populates="audit_logs")


# ==============================================================================
# ML RECOMMENDATION PIPELINE TABLES (Section 6: ML-01 to ML-12)
# ==============================================================================

class UserConsent(Base):
    """
    User Consent Entity: Tracks user privacy choices and tracking consents (ML-03, ML-07).
    When a user withdraws consent, future data collection and ML personalization respect this.
    """
    __tablename__ = "user_consents"
    __table_args__ = (
        UniqueConstraint("user_id", "consent_type", name="uq_user_consent_type"),
    )

    consent_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for consent entry",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Foreign key referencing consenting user",
    )
    consent_type: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="Consent domain: ML_PERSONALIZATION, ANALYTICS, LOCATION_TRACKING (ML-03)",
    )
    is_granted: Mapped[bool] = mapped_column(
        Boolean,
        default=True,
        nullable=False,
        comment="True if granted, False if withdrawn",
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
        comment="Timestamp of latest consent change",
    )

    user: Mapped["User"] = relationship(back_populates="consents")


class ActivityEvent(Base):
    """
    Activity Event Entity: Telemetry stream for user interactions with activities (ML-01, ML-02).
    Captures views, clicks, bookmarks, and joins with idempotency protection.
    """
    __tablename__ = "activity_events"
    __table_args__ = (
        Index("ix_activity_events_user_act", "user_id", "activity_id"),
        Index("ix_activity_events_idempotency", "idempotency_key"),
    )

    event_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for the telemetry event",
    )
    user_id: Mapped[str | None] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="SET NULL"),
        nullable=True,
        comment="User who triggered event (subject to consent rules ML-01)",
    )
    activity_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="CASCADE"),
        nullable=False,
        comment="Activity target of the interaction",
    )
    event_type: Mapped[str] = mapped_column(
        String(50),
        nullable=False,
        comment="Event action: VIEW, CLICK, SHARE, JOIN_REQUEST (ML-01, ML-02)",
    )
    idempotency_key: Mapped[str | None] = mapped_column(
        String(100),
        nullable=True,
        comment="Prevents duplicated telemetry during client network retries (ML-02)",
    )
    event_metadata: Mapped[dict | None] = mapped_column(
        JSON,
        nullable=True,
        comment="Session metadata, referrer surface, or dwell duration",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when interaction took place",
    )


class ModelVersion(Base):
    """
    Model Version Entity: Tracks registry of deployed ML algorithms and pipelines (ML-08, ML-09).
    """
    __tablename__ = "model_versions"

    model_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for model entry",
    )
    version_tag: Mapped[str] = mapped_column(
        String(50),
        unique=True,
        nullable=False,
        comment="Semantic version identifier (e.g. v2.1-two-tower-ranker ML-09)",
    )
    algorithm_name: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
        comment="Underlying algorithm or architecture (e.g. MatrixFactorization, GNN)",
    )
    hyperparameters: Mapped[dict | None] = mapped_column(
        JSON,
        nullable=True,
        comment="Training hyperparameters and feature weights",
    )
    is_active: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
        nullable=False,
        comment="Flag indicating active production serving model",
    )
    deployed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Deployment timestamp",
    )


class RecommendationFeedback(Base):
    """
    Recommendation Feedback Entity: Explicit & implicit feedback loop for ML training (ML-11).
    """
    __tablename__ = "recommendation_feedback"
    __table_args__ = (
        Index("ix_rec_feedback_user_act", "user_id", "activity_id"),
    )

    feedback_id: Mapped[str] = mapped_column(
        String(50),
        primary_key=True,
        default=generate_uuid,
        comment="Unique identifier for feedback entry",
    )
    recommendation_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("recommendations.recommendation_id", ondelete="CASCADE"),
        nullable=False,
        comment="Associated recommendation record",
    )
    user_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("users.user_id", ondelete="CASCADE"),
        nullable=False,
        comment="Target user who received the recommendation",
    )
    activity_id: Mapped[str] = mapped_column(
        String(50),
        ForeignKey("activities.activity_id", ondelete="CASCADE"),
        nullable=False,
        comment="Recommended activity",
    )
    feedback_type: Mapped[str] = mapped_column(
        String(30),
        nullable=False,
        comment="Feedback classification: CLICK, DISMISS, JOINED, RATED (ML-11)",
    )
    dwell_time_seconds: Mapped[int | None] = mapped_column(
        Integer,
        nullable=True,
        comment="Elapsed screen observation duration before interaction",
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        comment="Timestamp when feedback was registered",
    )

    recommendation: Mapped["Recommendation"] = relationship(back_populates="feedback_entries")
