UPDATE reference_data SET domain = 'SCREENING_OUTCOME_TYPE' where domain = 'OUTCOME_TYPE';

INSERT INTO reference_data
(domain, code, description, list_sequence, created_at, created_by, last_modified_at, last_modified_by, deactivated_at, deactivated_by)
VALUES
    ('DECISION_OUTCOME_TYPE', 'ACC', 'ACCT supporting', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, '2024-08-23 00:00:00', 'MOVE_AND_IMPROVE_TEAM'),
    ('DECISION_OUTCOME_TYPE', 'CUR', 'Progress to CSIP', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('DECISION_OUTCOME_TYPE', 'NFA', 'No further action', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('DECISION_OUTCOME_TYPE', 'WIN', 'Support outside of CSIP', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL);

alter table reference_data
    add constraint reference_data_domain_enum_check check
        (domain in ('AREA_OF_WORK', 'CONTRIBUTORY_FACTOR_TYPE', 'DECISION_OUTCOME_TYPE', 'DECISION_SIGNER_ROLE', 'INCIDENT_INVOLVEMENT', 'INCIDENT_LOCATION', 'INCIDENT_TYPE', 'INTERVIEWEE_ROLE', 'SCREENING_OUTCOME_TYPE'));