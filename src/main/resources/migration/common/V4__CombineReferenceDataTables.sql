-- Table for reference data
CREATE TABLE reference_data
(
    reference_data_id   BIGSERIAL PRIMARY KEY NOT NULL,
    domain              VARCHAR(30)           NOT NULL,
    code                VARCHAR(12)           NOT NULL,
    description         VARCHAR(40),
    list_sequence       INT,
    created_at          TIMESTAMP             NOT NULL,
    created_by          VARCHAR(32)           NOT NULL,
    last_modified_at    TIMESTAMP,
    last_modified_by    VARCHAR(32),
    deactivated_at      TIMESTAMP,
    deactivated_by      VARCHAR(32),
    UNIQUE(domain, code)
);

-- map foreign key constraints to new reference data table
ALTER TABLE referral
    DROP CONSTRAINT referral_incident_type_id_fkey,
    DROP CONSTRAINT referral_incident_location_id_fkey,
    DROP CONSTRAINT referral_referer_area_of_work_id_fkey,
    DROP CONSTRAINT referral_incident_involvement_id_fkey,
    ADD FOREIGN KEY (incident_type_id) REFERENCES reference_data (reference_data_id),
    ADD FOREIGN KEY (incident_location_id) REFERENCES reference_data (reference_data_id),
    ADD FOREIGN KEY (referer_area_of_work_id) REFERENCES reference_data (reference_data_id),
    ADD FOREIGN KEY (incident_involvement_id) REFERENCES reference_data (reference_data_id);

ALTER TABLE contributory_factor
    DROP CONSTRAINT contributory_factor_contributory_factor_type_id_fkey,
    ADD FOREIGN KEY (contributory_factor_type_id) REFERENCES reference_data (reference_data_id);

ALTER TABLE safer_custody_screening_outcome
    DROP CONSTRAINT safer_custody_screening_outcome_outcome_id_fkey,
    ADD FOREIGN KEY (outcome_id) REFERENCES reference_data (reference_data_id);

ALTER TABLE interview
    DROP CONSTRAINT interview_interviewee_role_id_fkey,
    ADD FOREIGN KEY (interviewee_role_id) REFERENCES reference_data (reference_data_id);

ALTER TABLE decisions_and_actions
    DROP CONSTRAINT decisions_and_actions_decision_outcome_id_fkey,
    DROP CONSTRAINT decisions_and_actions_decision_outcome_signed_off_by_role__fkey,
    ADD FOREIGN KEY (decision_outcome_id) REFERENCES reference_data (reference_data_id),
    ADD FOREIGN KEY (decision_outcome_signed_off_by_role_id) REFERENCES reference_data (reference_data_id);

-- drop deprecated reference data tables
DROP TABLE incident_type;
DROP TABLE incident_location;
DROP TABLE area_of_work;
DROP TABLE incident_involvement;
DROP TABLE contributory_factor_type;
DROP TABLE outcome;
DROP TABLE interviewee_role;
DROP TABLE role;