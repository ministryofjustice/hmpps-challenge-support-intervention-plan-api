DROP TABLE attendee; -- references review
DROP TABLE review; -- references plan
DROP TABLE identified_need; -- references plan
DROP TABLE interview; -- references investigation
DROP TABLE contributory_factor; -- references referral
DROP TABLE plan; -- remove shared PK
DROP TABLE decision_and_actions; -- remove shared PK, references referral
DROP TABLE investigation; -- remove shared PK, references referral
DROP TABLE safer_custody_screening_outcome; -- remove shared PK, reference referral
DROP TABLE referral; -- remove shared PK

-- Re-create entity with their own Primary Key instead of sharing csip_record's primary key:
-- Referral
-- Safer custody screening outcome
-- Investigation
-- Decision and actions
-- Plan

CREATE TABLE referral
(
    referral_id                        BIGSERIAL PRIMARY KEY NOT NULL,
    record_id                          BIGSERIAL NOT NULL,
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
    incident_involvement_id            INT,
    description_of_concern             TEXT,
    known_reasons                      TEXT,
    other_information                  TEXT,
    safer_custody_team_informed        BOOLEAN,
    referral_complete                  BOOLEAN,
    referral_completed_by              VARCHAR(32),
    referral_completed_by_display_name VARCHAR(255),
    referral_completed_date            DATE,
    FOREIGN KEY (incident_type_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (incident_location_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (referer_area_of_work_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (incident_involvement_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);
CREATE INDEX idx_referral_incident_type_id ON referral(incident_type_id);
CREATE INDEX idx_referral_incident_location_id ON referral(incident_location_id);
CREATE INDEX idx_referral_referer_area_of_work_id ON referral(referer_area_of_work_id);
CREATE INDEX idx_referral_incident_involvement_id ON referral(incident_involvement_id);
CREATE INDEX idx_referral_record_id ON referral(record_id);

CREATE TABLE safer_custody_screening_outcome
(
    safer_custody_screening_outcome_id  BIGSERIAL PRIMARY KEY NOT NULL,
    referral_id              BIGSERIAL NOT NULL,
    outcome_id               INT                   NOT NULL,
    recorded_by              VARCHAR(100)          NOT NULL,
    recorded_by_display_name VARCHAR(255)          NOT NULL,
    date                     DATE                  NOT NULL,
    reason_for_decision      TEXT                  NOT NULL,
    FOREIGN KEY (outcome_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (referral_id) references referral (referral_id)
);
CREATE INDEX idx_safer_custody_screening_outcome_outcome_id ON safer_custody_screening_outcome(outcome_id);
CREATE INDEX idx_safer_custody_screening_outcome_record_id ON safer_custody_screening_outcome(referral_id);


CREATE TABLE investigation
(
    investigation_id        BIGSERIAL PRIMARY KEY NOT NULL,
    referral_id             BIGSERIAL NOT NULL,
    staff_involved          TEXT,
    evidence_secured        TEXT,
    occurrence_reason       TEXT,
    persons_usual_behaviour TEXT,
    persons_trigger         TEXT,
    protective_factors      TEXT,
    FOREIGN KEY (referral_id) REFERENCES referral (referral_id)
);
CREATE INDEX idx_investigation_record_id ON investigation(referral_id);

CREATE TABLE decision_and_actions
(
    decision_and_actions_id                   BIGSERIAL PRIMARY KEY NOT NULL,
    referral_id                               BIGSERIAL NOT NULL,
    decision_conclusion                       TEXT,
    decision_outcome_id                       INT                   NOT NULL,
    decision_outcome_signed_off_by_role_id    INT,
    decision_outcome_recorded_by              VARCHAR(100),
    decision_outcome_recorded_by_display_name VARCHAR(255),
    decision_outcome_date                     DATE,
    next_steps                                TEXT,
    action_open_csip_alert                    BOOLEAN NOT NULL,
    action_non_associations_updated           BOOLEAN NOT NULL,
    action_observation_book                   BOOLEAN NOT NULL,
    action_unit_or_cell_move                  BOOLEAN NOT NULL,
    action_csra_or_rsra_review                BOOLEAN NOT NULL,
    action_service_referral                   BOOLEAN NOT NULL,
    action_sim_referral                       BOOLEAN NOT NULL,
    action_other                              TEXT,
    FOREIGN KEY (referral_id) REFERENCES referral (referral_id),
    FOREIGN KEY (decision_outcome_id) REFERENCES reference_data (reference_data_id),
    FOREIGN KEY (decision_outcome_signed_off_by_role_id) REFERENCES reference_data (reference_data_id)
);
CREATE INDEX idx_decision_and_actions_referral_id ON decision_and_actions(referral_id);
CREATE INDEX idx_decision_and_actions_decision_outcome_id ON decision_and_actions(decision_outcome_id);
CREATE INDEX idx_decision_and_actions_decision_outcome_signed_off_role_id ON decision_and_actions(decision_outcome_signed_off_by_role_id);

CREATE TABLE plan
(
    plan_id                BIGSERIAL PRIMARY KEY NOT NULL,
    record_id              BIGSERIAL NOT NULL,
    case_manager           VARCHAR(100)          NOT NULL,
    reason_for_plan        VARCHAR(240)          NOT NULL,
    first_case_review_date DATE                  NOT NULL,
    FOREIGN KEY (record_id) REFERENCES csip_record (record_id)
);
CREATE INDEX idx_plan_record_id ON plan(record_id);

-- Re-create child entity to reference to their direct parent, instead of csip_record
-- contributory_factor (referral)
-- interview (investigation)
-- identified_need (plan)
-- review (plan)
-- attendee (review)

CREATE TABLE contributory_factor
(
    contributory_factor_id        BIGSERIAL PRIMARY KEY NOT NULL,
    contributory_factor_uuid      UUID                  NOT NULL UNIQUE,
    referral_id                   BIGSERIAL             NOT NULL,
    contributory_factor_type_id   BIGSERIAL             NOT NULL,
    comment                       TEXT,
    created_at                    TIMESTAMP             NOT NULL,
    created_by                    VARCHAR(32)           NOT NULL,
    created_by_display_name       VARCHAR(255)          NOT NULL,
    last_modified_at              TIMESTAMP,
    last_modified_by              VARCHAR(32),
    last_modified_by_display_name VARCHAR(255),
    FOREIGN KEY (referral_id) REFERENCES referral (referral_id),
    FOREIGN KEY (contributory_factor_type_id) REFERENCES reference_data (reference_data_id)
);
CREATE INDEX idx_contributory_factor_record_id ON contributory_factor(referral_id);
CREATE INDEX idx_contributory_factor_contributory_factor_type_id ON contributory_factor(contributory_factor_type_id);


CREATE TABLE interview
(
    interview_id                  BIGSERIAL PRIMARY KEY NOT NULL,
    interview_uuid                UUID                  NOT NULL UNIQUE,
    investigation_id              BIGSERIAL             NOT NULL,
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
    FOREIGN KEY (investigation_id) REFERENCES investigation (investigation_id),
    FOREIGN KEY (interviewee_role_id) REFERENCES reference_data (reference_data_id)
);
CREATE INDEX idx_interview_record_id ON interview(investigation_id);
CREATE INDEX idx_interview_interviewee_role_id ON interview(interviewee_role_id);

CREATE TABLE identified_need
(
    identified_need_id            BIGSERIAL PRIMARY KEY NOT NULL,
    identified_need_uuid          UUID                  NOT NULL UNIQUE,
    plan_id                       BIGSERIAL             NOT NULL,
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
    FOREIGN KEY (plan_id) REFERENCES plan (plan_id)
);
CREATE INDEX idx_identified_need_record_id ON identified_need(plan_id);

CREATE TABLE review
(
    review_id                          BIGSERIAL PRIMARY KEY NOT NULL,
    review_uuid                        UUID                  NOT NULL UNIQUE,
    plan_id                            BIGSERIAL             NOT NULL,
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
    FOREIGN KEY (plan_id) REFERENCES plan (plan_id)
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
CREATE INDEX idx_review_record_id ON review(plan_id);
CREATE INDEX idx_attendee_review_id ON attendee(review_id);