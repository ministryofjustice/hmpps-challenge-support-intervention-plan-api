-- Import inactive reference data
INSERT INTO reference_data (domain, code, description, list_sequence, created_at, created_by, deactivated_at,
                            deactivated_by)
VALUES ('OUTCOME_TYPE', 'ACC', 'ACCT Supporting', 99, '2024-05-28 00:00:00', 'IMPORTED', '2024-05-28 00:00:00',
        'IMPORTED');

-- Import active reference data
INSERT INTO reference_data (domain, code, description, list_sequence, created_at, created_by)
VALUES ('CONTRIBUTORY_FACTOR_TYPE', 'AFL', 'Alcohol/Fermenting Liquid', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'ACT', 'Activities', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'CEN', 'Censors', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'OTHER', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'PERP', 'Perpetrator', 10, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_INVOLVEMENT', 'OTH', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'EDU', 'Education', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'EXY', 'Exercise Yard', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'CUR', 'Progress to CSIP', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'NFA', 'No Further Action', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'OPE', 'Progress to Investigation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'WIN', 'Support to be provided outside of CSIP', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('DECISION_SIGNER_ROLE', 'CUSTMAN', 'Custodial Manager', 10, '2024-05-28 00:00:00', 'IMPORTED'),
       ('DECISION_SIGNER_ROLE', 'THEHOFF', 'Head of Function', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'ATO', 'Abuse/Threats Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'WIT', 'Witness', 99, '2024-05-28 00:00:00', 'IMPORTED');
