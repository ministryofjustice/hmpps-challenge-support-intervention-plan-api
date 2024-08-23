-- Add inactive reference data for integration tests

INSERT INTO reference_data
    (domain, code, description, list_sequence, created_at, created_by, last_modified_at, last_modified_by, deactivated_at, deactivated_by)
VALUES
    ('CONTRIBUTORY_FACTOR_TYPE', 'CFT_INACT', 'Inactive contributory factor', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('AREA_OF_WORK', 'AOW_INACT', 'Inactive area of work', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('INTERVIEWEE_ROLE', 'IR_INACT', 'Inactive interviewee role', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('INCIDENT_INVOLVEMENT', 'II_INACT', 'Inactive incident involvement', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('INCIDENT_LOCATION', 'IL_INACT', 'Inactive incident location', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('OUTCOME_TYPE', 'OT_INACT', 'Inactive outcome type', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('DECISION_SIGNER_ROLE', 'DSR_INACT', 'Inactive decision signer role', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA'),
    ('INCIDENT_TYPE', 'IT_INACT', 'Inactive incident type', 99, now(), 'TEST_DATA', NULL, NULL, now(), 'TEST_DATA');
