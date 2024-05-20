CREATE TABLE csip_record
(
    record_id                     BIGSERIAL PRIMARY KEY NOT NULL,
    record_uuid                   UUID                  NOT NULL UNIQUE,
    prison_number                 VARCHAR(10)           NOT NULL,
    prison_code_when_recorded     VARCHAR(6),
    log_number                    VARCHAR(10),
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255)
);

CREATE TABLE incident_type
(
    incident_type_id BIGSERIAL PRIMARY KEY NOT NULL,
    code             VARCHAR(12)           NOT NULL UNIQUE,
    description      VARCHAR(40),
    list_sequence    INT,
    created_at       TIMESTAMP             NOT NULL,
    created_by       VARCHAR(32)           NOT NULL,
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(32),
    deactivated_at   TIMESTAMP,
    deactivated_by   VARCHAR(32)
);

CREATE TABLE incident_location
(
    incident_location_id BIGSERIAL PRIMARY KEY NOT NULL,
    code                 VARCHAR(40)           NOT NULL UNIQUE,
    description          VARCHAR(40),
    list_sequence        INT,
    created_at           TIMESTAMP             NOT NULL,
    created_by           VARCHAR(32)           NOT NULL,
    last_modified_at     TIMESTAMP,
    last_modified_by     VARCHAR(32),
    deactivated_at       TIMESTAMP,
    deactivated_by       VARCHAR(32)
);

CREATE TABLE area_of_work
(
    area_of_work_id  BIGSERIAL PRIMARY KEY NOT NULL,
    code             VARCHAR(40)           NOT NULL UNIQUE,
    description      VARCHAR(40),
    list_sequence    INT,
    created_at       TIMESTAMP             NOT NULL,
    created_by       VARCHAR(32)           NOT NULL,
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(32),
    deactivated_at   TIMESTAMP,
    deactivated_by   VARCHAR(32)
);

CREATE TABLE incident_involvement
(
    incident_involvement_id BIGSERIAL PRIMARY KEY NOT NULL,
    code                    VARCHAR(12)           NOT NULL UNIQUE,
    description             VARCHAR(40),
    list_sequence           INT,
    created_at              TIMESTAMP             NOT NULL,
    created_by              VARCHAR(32)           NOT NULL,
    last_modified_at        TIMESTAMP,
    last_modified_by        VARCHAR(32),
    deactivated_at          TIMESTAMP,
    deactivated_by          varchar(32)
);


-- Table for contributory factor types
CREATE TABLE contributory_factor_type
(
    contributory_factor_type_id BIGSERIAL PRIMARY KEY NOT NULL,
    code                        VARCHAR(12)           NOT NULL UNIQUE,
    description                 VARCHAR(40),
    list_sequence               INT,
    created_at                  TIMESTAMP             NOT NULL,
    created_by                  VARCHAR(32)           NOT NULL,
    last_modified_at            TIMESTAMP,
    last_modified_by            VARCHAR(32),
    deactivated_at              TIMESTAMP,
    deactivated_by              VARCHAR(32)
);

-- Table for outcomes
CREATE TABLE outcome
(
    outcome_id       BIGSERIAL PRIMARY KEY NOT NULL,
    code             VARCHAR(12)           NOT NULL UNIQUE,
    description      VARCHAR(40),
    list_sequence    INT,
    created_at       TIMESTAMP             NOT NULL,
    created_by       VARCHAR(32)           NOT NULL,
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(32),
    deactivated_at   TIMESTAMP,
    deactivated_by   VARCHAR(32)
);

-- Table for interviewee roles
CREATE TABLE interviewee_role
(
    interviewee_role_id BIGSERIAL PRIMARY KEY NOT NULL,
    code                VARCHAR(12)           NOT NULL UNIQUE,
    description         VARCHAR(40),
    list_sequence       INT,
    created_at          TIMESTAMP             NOT NULL,
    created_by          VARCHAR(32)           NOT NULL,
    last_modified_at    TIMESTAMP,
    last_modified_by    VARCHAR(32),
    deactivated_at      TIMESTAMP,
    deactivated_by      VARCHAR(32)
);

-- Table for roles
CREATE TABLE role
(
    role_id          BIGSERIAL PRIMARY KEY NOT NULL,
    code             VARCHAR(12)           NOT NULL UNIQUE,
    description      VARCHAR(40),
    list_sequence    INT,
    created_at       TIMESTAMP             NOT NULL,
    created_by       VARCHAR(32)           NOT NULL,
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(32),
    deactivated_at   TIMESTAMP,
    deactivated_by   VARCHAR(32)
);

CREATE TABLE referral
(
    record_id                          BIGSERIAL PRIMARY KEY NOT NULL,
    incident_date                      DATE                  NOT NULL,
    incident_time                      TIME,
    incident_type_id                   INT                   NOT NULL,
    incident_location_id               INT                   NOT NULL,
    referred_by                        VARCHAR(240)          NOT NULL,
    referer_area_of_work_id            INT                   NOT NULL,
    referral_date                      DATE                  NOT NULL,
    referral_summary                   TEXT,
    proactive_referral                 BOOLEAN,
    staff_assaulted                    BOOLEAN,
    assaulted_staff_name               TEXT,
    release_date                       DATE,
    incident_involvement_id            INT                   NOT NULL,
    description_of_concern             TEXT                  NOT NULL,
    known_reasons                      TEXT                  NOT NULL,
    other_information                  TEXT,
    safer_custody_team_informed        BOOLEAN,
    referral_complete                  BOOLEAN,
    referral_completed_by              VARCHAR(32),
    referral_completed_by_display_name VARCHAR(255),
    referral_completed_date            DATE,
    FOREIGN KEY (incident_type_id) REFERENCES incident_type (incident_type_id),
    FOREIGN KEY (incident_location_id) REFERENCES incident_location (incident_location_id),
    FOREIGN KEY (referer_area_of_work_id) REFERENCES area_of_work (area_of_work_id),
    FOREIGN KEY (incident_involvement_id) REFERENCES incident_involvement (incident_involvement_id),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);

CREATE TABLE contributory_factor
(
    contributory_factor_id        BIGSERIAL PRIMARY KEY NOT NULL,
    contributory_factor_uuid      UUID                  NOT NULL UNIQUE,
    record_id                     BIGSERIAL             NOT NULL,
    contributory_factor_type_id   BIGSERIAL             NOT NULL,
    comment                       TEXT,
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id),
    FOREIGN KEY (contributory_factor_type_id) REFERENCES contributory_factor_type (contributory_factor_type_id)
);

CREATE TABLE safer_custody_screening_outcome
(
    record_id                BIGSERIAL PRIMARY KEY NOT NULL,
    outcome_id               INT                   NOT NULL,
    recorded_by              VARCHAR(100)          NOT NULL,
    recorded_by_display_name VARCHAR(255)          NOT NULL,
    date                     DATE                  NOT NULL,
    reason_for_decision      TEXT                  NOT NULL,
    FOREIGN KEY (outcome_id) REFERENCES outcome (outcome_id),
    FOREIGN KEY (record_id) references csip_record (record_id)
);

CREATE TABLE investigation
(
    record_id               BIGSERIAL PRIMARY KEY NOT NULL,
    staff_involved          TEXT,
    evidence_secured        TEXT,
    occurrence_reason       TEXT,
    persons_usual_behaviour TEXT,
    persons_trigger         TEXT,
    protective_factors      TEXT,
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);

CREATE TABLE interview
(
    interview_id                  BIGSERIAL PRIMARY KEY NOT NULL,
    interview_uuid                UUID                  NOT NULL UNIQUE,
    record_id                     BIGSERIAL             NOT NULL,
    interviewee                   VARCHAR(100)          NOT NULL,
    interview_date                DATE                  NOT NULL,
    interviewee_role_id           INT                   NOT NULL,
    interview_text                TEXT,
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id),
    FOREIGN KEY (interviewee_role_id) REFERENCES interviewee_role (interviewee_role_id)
);

CREATE TABLE decisions_and_actions
(
    record_id                                 BIGSERIAL PRIMARY KEY NOT NULL,
    decision_conclusion                       TEXT,
    decision_outcome_id                       INT                   NOT NULL,
    decision_outcome_signed_off_by_role_id    INT,
    decision_outcome_recorded_by              VARCHAR(100),
    decision_outcome_recorded_by_display_name VARCHAR(255),
    decision_outcome_date                     DATE,
    next_steps                                TEXT,
    action_open_csip_alert                    BOOLEAN,
    action_non_associations_updated           BOOLEAN,
    action_observation_book                   BOOLEAN,
    action_unit_or_cell_move                  BOOLEAN,
    action_csra_or_rsra_review                BOOLEAN,
    action_service_referral                   BOOLEAN,
    action_sim_referral                       BOOLEAN,
    action_other                              TEXT,
    FOREIGN KEY (decision_outcome_id) REFERENCES outcome (outcome_id),
    FOREIGN KEY (decision_outcome_signed_off_by_role_id) REFERENCES role (role_id)
);

CREATE TABLE plan
(
    record_id              BIGSERIAL PRIMARY KEY NOT NULL,
    case_manager           VARCHAR(100)          NOT NULL,
    reason_for_plan        VARCHAR(240)          NOT NULL,
    first_case_review_date DATE                  NOT NULL,
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);

CREATE TABLE identified_need
(
    identified_need_id            BIGSERIAL PRIMARY KEY NOT NULL,
    identified_need_uuid          UUID                  NOT NULL UNIQUE,
    record_id                     BIGSERIAL             NOT NULL,
    identified_need               TEXT                  NOT NULL,
    need_identified_by            VARCHAR(100)          NOT NULL,
    created_date                  DATE                  NOT NULL,
    target_date                   DATE                  NOT NULL,
    closed_date                   DATE,
    intervention                  TEXT                  NOT NULL,
    progression                   TEXT,
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);

CREATE TABLE review
(
    review_id                          BIGSERIAL PRIMARY KEY NOT NULL,
    review_uuid                        UUID                  NOT NULL UNIQUE,
    record_id                          BIGSERIAL             NOT NULL,
    review_sequence                    INT                   NOT NULL,
    review_date                        DATE,
    recorded_by                        VARCHAR(32)           NOT NULL,
    recorded_by_display_name           VARCHAR(255)          NOT NULL,
    next_review_date                   DATE,
    action_responsible_people_informed BOOLEAN,
    action_csip_updated                BOOLEAN,
    action_remain_on_csip              BOOLEAN,
    action_case_note                   BOOLEAN,
    action_close_csip                  BOOLEAN,
    csip_closed_date                   DATE,
    summary                            TEXT,
    created_at                         TIMESTAMP             NOT NULL,
    created_by                         VARCHAR(32)           NOT NULL,
    created_by_display_name            VARCHAR(255)          NOT NULL,
    last_modified_at                   TIMESTAMP,
    last_modified_by                   VARCHAR(32),
    last_modified_by_display_name      VARCHAR(255),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);

CREATE TABLE attendee
(
    attendee_id                   BIGSERIAL PRIMARY KEY NOT NULL,
    attendee_uuid                 UUID                  NOT NULL UNIQUE,
    review_id                     BIGSERIAL             NOT NULL,
    name                          VARCHAR(100),
    role                          VARCHAR(50),
    attended                      BOOLEAN,
    contribution                  TEXT,
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255),
    FOREIGN KEY (review_id) REFERENCES review (review_id)
);

CREATE TABLE audit_event
(
    id                                       BIGSERIAL PRIMARY KEY NOT NULL,
    csip_record_id                           BIGSERIAL             NOT NULL,
    action                                   VARCHAR(40)           NOT NULL,
    description                              TEXT                  NOT NULL,
    actioned_at                              TIMESTAMP             NOT NULL,
    actioned_by                              VARCHAR(32)           NOT NULL,
    actioned_by_captured_name                VARCHAR(255)          NOT NULL,
    record_affected                          BOOLEAN,
    referral_affected                        BOOLEAN,
    contributory_factor_affected             BOOLEAN,
    safer_custody_screening_outcome_affected BOOLEAN,
    investigation_affected                   BOOLEAN,
    interview_affected                       BOOLEAN,
    decisions_and_actions_affected           BOOLEAN,
    plan_affected                            BOOLEAN,
    identified_need_affected                 BOOLEAN,
    review_affected                          BOOLEAN,
    attendee_affected                        BOOLEAN,
    FOREIGN KEY (csip_record_id) REFERENCES csip_record (record_id)
);
