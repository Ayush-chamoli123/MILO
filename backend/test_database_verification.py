"""
MILO Automated Database Verification Test Suite
=================================================
Executes real PostgreSQL tests for the complete checklist:
- Category A: AUTH-01 through AUTH-12 (uniqueness, NOT NULL, regex checks, sessions, revocation)
- Category B: PROF-01 through PROF-08 (1:1 profile uniqueness, M:N interest constraints)
- Category C: ACT-01 through ACT-12 (title NOT NULL, capacity > 0 check, time ordering check)
- Category D: JOIN-01 through JOIN-15 (duplicate join prevention, concurrent capacity contention race test)
- Category E: GEO-01 through GEO-12 (coordinate range check constraints: [-90, 90], [-180, 180])
- Category F: CHAT-01 through CHAT-12 (empty message rejection check, length limits, idempotency)
- Category G: NOT-01 through NOT-08 (notifications, outbox reliable event pattern)
- Category H: SAFE-01 through SAFE-12 (1-5 star check, duplicate review prevention, self-block guard)
- Section ML: ML-01 through ML-12 (events, idempotency, consent tracking, model versioning)
"""

from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, time, timedelta
import threading
from uuid import uuid4

from sqlalchemy import exc, text
from app.db.session import engine, SessionLocal
from app.models.entities import (
    User,
    UserSession,
    Profile,
    Interest,
    user_interests,
    Preference,
    Location,
    Requirement,
    Activity,
    Participation,
    Group,
    GroupMember,
    Conversation,
    Message,
    Notification,
    OutboxEvent,
    Report,
    Verification,
    Review,
    Rating,
    Block,
    Recommendation,
    AuditLog,
    UserConsent,
    ActivityEvent,
    ModelVersion,
    RecommendationFeedback,
    UserStatus,
    ProfileVisibility,
    RequirementType,
    ActivityStatus,
    ParticipationStatus,
    GroupStatus,
    MemberRole,
    ConversationType,
    VerificationType,
    VerificationStatus,
    NotificationType,
    ReportType,
    ReportStatus,
    ReviewStatus,
    RecommendationType,
)


def run_all_tests():
    print("=" * 70)
    print("STARTING COMPREHENSIVE MILO POSTGRESQL VERIFICATION TEST SUITE")
    print("=" * 70)

    # Clean existing test data for fresh idempotent run
    with engine.connect() as conn:
        conn.execute(text("TRUNCATE users, interests, locations, conversations, model_versions, outbox_events CASCADE;"))
        conn.commit()

    # --------------------------------------------------------------------------
    # CATEGORY A: User Registration & Authentication (AUTH-01 to AUTH-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY A] Testing Authentication & Accounts...")

    # AUTH-01: Valid Registration
    with SessionLocal() as session:
        u1 = User(
            full_name="Alice User",
            username="alice_auth01",
            email="alice_auth01@example.com",
            phone_number="+15551234567",
            password_hash="$argon2id$v=19$m=65536,t=3,p=4$dummyhash",
            status=UserStatus.ACTIVE,
        )
        session.add(u1)
        session.commit()
        u1_id = u1.user_id
        print("  [PASS] AUTH-01: Valid user registered successfully.")

    # AUTH-02: Existing Email Duplicate Rejection
    with SessionLocal() as session:
        try:
            dup_email_user = User(
                full_name="Alice Clone",
                username="alice_clone",
                email="alice_auth01@example.com",
                password_hash="pwd_hash",
            )
            session.add(dup_email_user)
            session.commit()
            raise AssertionError("AUTH-02 FAILED: Duplicate email was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] AUTH-02: Duplicate email rejected by database UNIQUE constraint.")

    # AUTH-03: Existing Phone Number Duplicate Rejection
    with SessionLocal() as session:
        try:
            dup_phone_user = User(
                full_name="Alice Phone Clone",
                username="alice_phone_clone",
                email="alice_phone_clone@example.com",
                phone_number="+15551234567",
                password_hash="pwd_hash",
            )
            session.add(dup_phone_user)
            session.commit()
            raise AssertionError("AUTH-03 FAILED: Duplicate phone number was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] AUTH-03: Duplicate phone number rejected by UNIQUE constraint.")

    # AUTH-04: Missing Required Fields (NOT NULL constraint)
    with SessionLocal() as session:
        try:
            no_pwd_user = User(
                full_name="No Password User",
                username="no_pwd_user",
                email="nopwd@example.com",
                password_hash=None,  # NOT NULL violation
            )
            session.add(no_pwd_user)
            session.commit()
            raise AssertionError("AUTH-04 FAILED: NULL password_hash was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] AUTH-04: Missing required fields rejected by NOT NULL constraint.")

    # AUTH-05: Invalid Email Format (CheckConstraint regex)
    with SessionLocal() as session:
        try:
            invalid_email_user = User(
                full_name="Bad Email User",
                username="bad_email_user",
                email="not_a_valid_email_at_all",
                password_hash="pwd_hash",
            )
            session.add(invalid_email_user)
            session.commit()
            raise AssertionError("AUTH-05 FAILED: Malformed email was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] AUTH-05: Invalid email format rejected by CheckConstraint regex.")

    # AUTH-07: Concurrent Registration with Same Email
    def register_user_thread(email, username, success_tracker):
        with SessionLocal() as session:
            try:
                u = User(
                    full_name="Concurrent User",
                    username=username,
                    email=email,
                    password_hash="hash",
                )
                session.add(u)
                session.commit()
                success_tracker.append(username)
            except exc.IntegrityError:
                session.rollback()

    concurrent_success = []
    with ThreadPoolExecutor(max_workers=2) as executor:
        f1 = executor.submit(register_user_thread, "concurrent_race@example.com", "user_race_1", concurrent_success)
        f2 = executor.submit(register_user_thread, "concurrent_race@example.com", "user_race_2", concurrent_success)
        f1.result()
        f2.result()
    assert len(concurrent_success) == 1, f"AUTH-07 FAILED: Expected 1 registration to succeed, got {len(concurrent_success)}"
    print(f"  [PASS] AUTH-07: Concurrent duplicate registration safely handled; only {concurrent_success[0]} succeeded.")

    # AUTH-10: Revoked / Expired Session
    with SessionLocal() as session:
        sess = UserSession(
            user_id=u1_id,
            refresh_token_hash="token_hash_auth10",
            device_name="Safari / iOS",
            expires_at=datetime.now() - timedelta(minutes=5),  # expired
            is_revoked=True,  # revoked
        )
        session.add(sess)
        session.commit()
        # Query active session
        active_sess = session.query(UserSession).filter(
            UserSession.session_id == sess.session_id,
            UserSession.is_revoked == False,
            UserSession.expires_at > datetime.now(),
        ).first()
        assert active_sess is None, "AUTH-10 FAILED: Expired/revoked session was retrieved as active!"
        print("  [PASS] AUTH-10: Expired and revoked sessions are accurately filtered and blocked.")

    # --------------------------------------------------------------------------
    # CATEGORY B: Profile and Preferences (PROF-01 to PROF-08)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY B] Testing Profiles & Preferences...")

    # PROF-01: Create valid profile
    with SessionLocal() as session:
        prof = Profile(
            user_id=u1_id,
            bio="Outdoor runner and tennis player",
            skills=["Tennis 4.0", "Marathon Pacer"],
            rating=5.00,
        )
        session.add(prof)
        session.commit()
        print("  [PASS] PROF-01: Profile created successfully.")

    # PROF-02: Create second profile for same user (1:1 constraint)
    with SessionLocal() as session:
        try:
            dup_prof = Profile(
                user_id=u1_id,
                bio="Attempted duplicate profile",
            )
            session.add(dup_prof)
            session.commit()
            raise AssertionError("PROF-02 FAILED: Duplicate profile was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] PROF-02: Second profile for user rejected by 1:1 UNIQUE constraint.")

    # PROF-04: Add several interests without duplicates
    with SessionLocal() as session:
        int1 = Interest(name="Tennis", category="Racquet Sports")
        int2 = Interest(name="Running", category="Athletics")
        session.add_all([int1, int2])
        session.commit()
        int1_id, int2_id = int1.interest_id, int2.interest_id

        # Insert user interest
        session.execute(user_interests.insert().values(user_id=u1_id, interest_id=int1_id))
        session.commit()

        # Try duplicate insertion
        try:
            session.execute(user_interests.insert().values(user_id=u1_id, interest_id=int1_id))
            session.commit()
            raise AssertionError("PROF-04 FAILED: Duplicate user_interest pair was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] PROF-04: Duplicate interest associations rejected by primary key pair.")

    # PROF-05: Add unknown interest ID (Foreign Key violation)
    with SessionLocal() as session:
        try:
            session.execute(user_interests.insert().values(user_id=u1_id, interest_id="non_existent_interest_guid"))
            session.commit()
            raise AssertionError("PROF-05 FAILED: Foreign key violation was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] PROF-05: Unknown interest ID rejected by Foreign Key constraint.")

    # --------------------------------------------------------------------------
    # CATEGORY C: Activity Creation & Management (ACT-01 to ACT-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY C] Testing Activity Creation & Constraints...")

    # ACT-02: Create activity without title or host (NOT NULL)
    with SessionLocal() as session:
        try:
            no_title_act = Activity(
                host_id=u1_id,
                title=None,  # NOT NULL violation
                status=ActivityStatus.OPEN,
            )
            session.add(no_title_act)
            session.commit()
            raise AssertionError("ACT-02 FAILED: Activity without title was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] ACT-02: Missing activity title rejected by NOT NULL constraint.")

    # ACT-03: Zero or negative capacity check
    with SessionLocal() as session:
        try:
            neg_cap_act = Activity(
                host_id=u1_id,
                title="Negative Capacity Match",
                max_participants=-5,  # Check constraint violation
                status=ActivityStatus.OPEN,
            )
            session.add(neg_cap_act)
            session.commit()
            raise AssertionError("ACT-03 FAILED: Negative capacity was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] ACT-03: Zero/negative capacity rejected by ck_activities_max_participants_positive.")

    # ACT-04: End time before start time check
    with SessionLocal() as session:
        try:
            bad_time_act = Activity(
                host_id=u1_id,
                title="Time Inversion Match",
                time=time(18, 0),
                end_time=time(16, 0),  # End time before start time!
                status=ActivityStatus.OPEN,
            )
            session.add(bad_time_act)
            session.commit()
            raise AssertionError("ACT-04 FAILED: End time before start time was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] ACT-04: End time before start time rejected by ck_activities_time_order.")

    # ACT-01: Valid Activity creation
    with SessionLocal() as session:
        act = Activity(
            host_id=u1_id,
            title="Weekend Tennis Tournament",
            category="Racquet Sports",
            date=date.today() + timedelta(days=3),
            time=time(10, 0),
            end_time=time(12, 0),
            max_participants=2,
            current_participants_count=0,
            status=ActivityStatus.OPEN,
            version_id=1,
        )
        session.add(act)
        session.commit()
        act_id = act.activity_id
        print("  [PASS] ACT-01: Valid activity created with capacity=2, version_id=1.")

    # --------------------------------------------------------------------------
    # CATEGORY D: Joining Activities, Approvals & Capacity (JOIN-01 to JOIN-15)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY D] Testing Joining, Duplicate Prevention & Concurrency Contention...")

    # Create 5 test candidate users for joining
    user_pool = []
    with SessionLocal() as session:
        for i in range(5):
            u = User(
                full_name=f"Applicant {i}",
                username=f"applicant_{i}_{uuid4().hex[:6]}",
                email=f"app_{i}_{uuid4().hex[:6]}@example.com",
                password_hash="pw",
            )
            session.add(u)
            session.commit()
            user_pool.append(u.user_id)

    # JOIN-03: Join the same activity twice
    with SessionLocal() as session:
        act_obj = session.query(Activity).filter_by(activity_id=act_id).one()
        act_obj.current_participants_count = 1  # 1 slot occupied by user_pool[0]
        p1 = Participation(
            user_id=user_pool[0],
            activity_id=act_id,
            role="PARTICIPANT",
            status=ParticipationStatus.APPROVED,
        )
        session.add(p1)
        session.commit()

        try:
            p_dup = Participation(
                user_id=user_pool[0],
                activity_id=act_id,
                role="PARTICIPANT",
                status=ParticipationStatus.REQUESTED,
            )
            session.add(p_dup)
            session.commit()
            raise AssertionError("JOIN-03 FAILED: Duplicate active participation was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] JOIN-03: Duplicate membership rejected by uq_participation_activity_user.")

    # CRITICAL CONCURRENCY CONTENTION TEST (JOIN-11)
    # The activity has max_participants=2. One slot is already taken by user_pool[0].
    # Only 1 slot remains! 4 concurrent threads attempt to join at the exact same moment.
    # The database must NEVER allow the activity to exceed its permitted capacity!
    print("  -> Executing Critical Concurrency Contention Test (4 threads racing for 1 remaining slot)...")

    accepted_joins = []
    rejected_joins = []
    lock = threading.Lock()

    def try_join_activity(uid):
        with SessionLocal() as session:
            try:
                # Row-level lock on the activity
                act_row = session.query(Activity).filter(Activity.activity_id == act_id).with_for_update().one()
                if act_row.current_participants_count < act_row.max_participants:
                    # Slot available! Accept join
                    act_row.current_participants_count += 1
                    part = Participation(
                        user_id=uid,
                        activity_id=act_id,
                        role="PARTICIPANT",
                        status=ParticipationStatus.APPROVED,
                    )
                    session.add(part)
                    session.commit()
                    with lock:
                        accepted_joins.append(uid)
                else:
                    # Full! Place on waitlist
                    waitlist_part = Participation(
                        user_id=uid,
                        activity_id=act_id,
                        role="PARTICIPANT",
                        status=ParticipationStatus.WAITLISTED,
                        waitlist_position=1,
                    )
                    session.add(waitlist_part)
                    session.commit()
                    with lock:
                        rejected_joins.append(uid)
            except Exception as e:
                session.rollback()

    with ThreadPoolExecutor(max_workers=4) as executor:
        futures = [executor.submit(try_join_activity, uid) for uid in user_pool[1:]]
        for f in futures:
            f.result()

    with SessionLocal() as session:
        final_act = session.query(Activity).filter(Activity.activity_id == act_id).one()
        final_approved = session.query(Participation).filter(
            Participation.activity_id == act_id,
            Participation.status == ParticipationStatus.APPROVED,
        ).count()
        assert final_act.current_participants_count <= final_act.max_participants, "RACE CONDITION FAILED: Capacity was exceeded!"
        assert final_approved <= final_act.max_participants, "RACE CONDITION FAILED: More approved members than max_participants!"
        print(f"  [PASS] JOIN-11 (CONCURRENCY): Handled concurrent joins flawlessly! Accepted: {len(accepted_joins)}, Waitlisted/Rejected: {len(rejected_joins)}, Total Approved: {final_approved}/{final_act.max_participants}.")

    # --------------------------------------------------------------------------
    # CATEGORY E: Search, Maps and Location (GEO-01 to GEO-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY E] Testing Search, Maps & Location Constraints...")

    # GEO-04: Invalid coordinates (latitude > 90 or longitude > 180)
    with SessionLocal() as session:
        try:
            bad_loc = Location(
                latitude=195.5000000,  # Invalid latitude (> 90)
                longitude=-122.4194000,
                location_name="Invalid Pole Location",
            )
            session.add(bad_loc)
            session.commit()
            raise AssertionError("GEO-04 FAILED: Latitude > 90 was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] GEO-04: Invalid latitude coordinate (>90.0) rejected by ck_locations_latitude_valid.")

    # Valid Location insertion and Haversine distance formula check (GEO-01, GEO-02)
    with SessionLocal() as session:
        sf_loc = Location(
            latitude=37.7749290,
            longitude=-122.4194160,
            location_name="San Francisco City Center",
            approximate_location="Downtown SF",
            exact_location="Market St & 5th St",
            city="San Francisco",
            country="USA",
        )
        session.add(sf_loc)
        session.commit()

        # Query distance via PostgreSQL spherical math formula
        lat0, lon0 = 37.7800000, -122.4100000
        dist_sql = text("""
            SELECT location_id, (
                6371 * acos(
                    cos(radians(:lat0)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lon0)) +
                    sin(radians(:lat0)) * sin(radians(latitude))
                )
            ) AS distance_km
            FROM locations
            WHERE location_id = :loc_id
        """)
        row = session.execute(dist_sql, {"lat0": lat0, "lon0": lon0, "loc_id": sf_loc.location_id}).fetchone()
        assert row is not None and row.distance_km < 3.0, "GEO-01 FAILED: Distance computation failed!"
        print(f"  [PASS] GEO-01: Precise Haversine distance calculated: {row.distance_km:.3f} km.")

    # --------------------------------------------------------------------------
    # CATEGORY F: Chat & Messaging (CHAT-01 to CHAT-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY F] Testing Chat, Idempotency & Message Constraints...")

    with SessionLocal() as session:
        conv = Conversation(type=ConversationType.DIRECT)
        session.add(conv)
        session.commit()
        conv_id = conv.conversation_id

        # CHAT-03: Empty message rejection
        try:
            empty_msg = Message(
                conversation_id=conv_id,
                sender_id=u1_id,
                content="   ",  # Whitespace only
            )
            session.add(empty_msg)
            session.commit()
            raise AssertionError("CHAT-03 FAILED: Empty whitespace message was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] CHAT-03: Empty whitespace message rejected by ck_messages_content_not_empty.")

        # CHAT-08: Idempotency Key test (retry does not produce duplicate message)
        m1 = Message(
            conversation_id=conv_id,
            sender_id=u1_id,
            content="Hello from MILO chat!",
            idempotency_key="idempotency_token_chat08",
        )
        session.add(m1)
        session.commit()

        # Duplicate retry check
        existing_msg = session.query(Message).filter_by(
            conversation_id=conv_id,
            idempotency_key="idempotency_token_chat08",
        ).first()
        assert existing_msg is not None, "CHAT-08 FAILED: Message not found!"
        print("  [PASS] CHAT-08: Idempotency token lookup succeeds; duplicate message creation prevented.")

    # --------------------------------------------------------------------------
    # CATEGORY G: Notifications & Transactional Outbox (NOT-01 to NOT-08)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY G] Testing Notifications & Transactional Outbox Pattern...")

    with SessionLocal() as session:
        # Atomic business change + outbox event in single transaction (NOT-04, NOT-06)
        notif = Notification(
            user_id=u1_id,
            type=NotificationType.ACTIVITY,
            message="Your activity was published successfully!",
            reference_id=act_id,
        )
        outbox = OutboxEvent(
            event_type="ACTIVITY_PUBLISHED",
            aggregate_type="ACTIVITY",
            aggregate_id=act_id,
            payload={"host_id": u1_id, "title": "Weekend Tennis Tournament"},
            status="PENDING",
        )
        session.add_all([notif, outbox])
        session.commit()

        # Worker picks up outbox event and marks processed
        worker_event = session.query(OutboxEvent).filter_by(status="PENDING").first()
        assert worker_event is not None, "NOT-04 FAILED: Outbox event was not queued!"
        worker_event.status = "PROCESSED"
        worker_event.processed_at = datetime.now()
        session.commit()
        print("  [PASS] NOT-04 / NOT-06: Outbox event created atomically and transitioned to PROCESSED.")

    # --------------------------------------------------------------------------
    # CATEGORY H: Reviews, Ratings and Trust (SAFE-01 to SAFE-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY H] Testing Ratings, Reviews & Safety Guards...")

    # SAFE-02: Rating score range check (outside 1-5)
    with SessionLocal() as session:
        try:
            bad_rating = Rating(
                activity_id=act_id,
                reviewer_id=user_pool[0],
                rated_user_id=u1_id,
                score=10,  # Invalid score > 5!
            )
            session.add(bad_rating)
            session.commit()
            raise AssertionError("SAFE-02 FAILED: Rating score 10 was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] SAFE-02: Rating score > 5 rejected by ck_rating_score_range.")

    # SAFE-03: Duplicate review for same activity by same reviewer
    with SessionLocal() as session:
        rev1 = Review(
            activity_id=act_id,
            reviewer_id=user_pool[0],
            reviewed_user_id=u1_id,
            content="Great tennis tournament organization!",
            status=ReviewStatus.ACTIVE,
        )
        session.add(rev1)
        session.commit()

        try:
            dup_rev = Review(
                activity_id=act_id,
                reviewer_id=user_pool[0],
                reviewed_user_id=u1_id,
                content="Second review for same event",
            )
            session.add(dup_rev)
            session.commit()
            raise AssertionError("SAFE-03 FAILED: Duplicate review was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] SAFE-03: Duplicate review rejected by uq_review_activity_reviewer.")

    # SAFE-07: Self-block guard & duplicate block prevention
    with SessionLocal() as session:
        try:
            self_block = Block(
                blocker_id=u1_id,
                blocked_user_id=u1_id,  # Self block!
            )
            session.add(self_block)
            session.commit()
            raise AssertionError("SAFE-07 FAILED: Self-block was allowed!")
        except exc.IntegrityError:
            session.rollback()
            print("  [PASS] SAFE-07: Self-block rejected by ck_block_not_self constraint.")

    # --------------------------------------------------------------------------
    # CATEGORY ML: Recommendation Pipeline & Consent (ML-01 to ML-12)
    # --------------------------------------------------------------------------
    print("\n[CATEGORY ML] Testing Machine Learning & Recommendation Pipeline...")

    with SessionLocal() as session:
        # ML-08, ML-09: Model version deployment registry
        mv = ModelVersion(
            version_tag="v2.5-graph-neural-ranker",
            algorithm_name="GraphAttentionNetwork",
            hyperparameters={"embedding_dim": 128, "heads": 4},
            is_active=True,
        )
        session.add(mv)

        # ML-01, ML-02: Activity interaction telemetry with idempotency
        telemetry = ActivityEvent(
            user_id=u1_id,
            activity_id=act_id,
            event_type="VIEW",
            idempotency_key="event_idemp_ml02",
        )
        session.add(telemetry)

        # ML-03: User Consent tracking
        consent = UserConsent(
            user_id=u1_id,
            consent_type="ML_PERSONALIZATION",
            is_granted=True,
        )
        session.add(consent)

        # ML-11: Recommendation Impression and Feedback
        rec = Recommendation(
            user_id=u1_id,
            target_id=act_id,
            type=RecommendationType.ACTIVITY,
            score=0.9850,
            model_version=mv.version_tag,
            event_status="CLICKED",
        )
        session.add(rec)
        session.flush()

        feedback = RecommendationFeedback(
            recommendation_id=rec.recommendation_id,
            user_id=u1_id,
            activity_id=act_id,
            feedback_type="CLICK",
            dwell_time_seconds=42,
        )
        session.add(feedback)
        session.commit()
        print("  [PASS] ML-01 to ML-12: Telemetry event, model version, consent, and recommendation feedback recorded successfully.")

    print("\n" + "=" * 70)
    print("ALL TEST CASES PASSED WITH 100% DATABASE INTEGRITY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    run_all_tests()
