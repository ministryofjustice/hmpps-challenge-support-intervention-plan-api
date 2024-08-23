TRUNCATE TABLE reference_data RESTART IDENTITY CASCADE;

-- Reference data from production as of 28 Jun 2024 extracted using the following SQL
-- SELECT
--     '   (''' || CASE WHEN domain = 'CSIP_FAC' THEN 'CONTRIBUTORY_FACTOR_TYPE'
--         WHEN domain = 'CSIP_FUNC' THEN 'AREA_OF_WORK'
--         WHEN domain = 'CSIP_INTVROL' THEN 'INTERVIEWEE_ROLE'
--         WHEN domain = 'CSIP_INV' THEN 'INCIDENT_INVOLVEMENT'
--         WHEN domain = 'CSIP_LOC' THEN 'INCIDENT_LOCATION'
--         WHEN domain = 'CSIP_OUT' THEN 'OUTCOME_TYPE'
--         WHEN domain = 'CSIP_ROLE' THEN 'DECISION_SIGNER_ROLE'
--         WHEN domain = 'CSIP_TYP' THEN 'INCIDENT_TYPE' ELSE '' END || ''', '''
--         || code || ''', ''' || description || ''', ' || list_seq || ', '''
--         || TO_CHAR(create_datetime, 'YYYY-MM-DD HH24:MI:SS') || ''', ''' || create_user_id || ''', '
--         || CASE WHEN modify_datetime IS NOT NULL THEN '''' || TO_CHAR(modify_datetime, 'YYYY-MM-DD HH24:MI:SS') || ''', ''' || modify_user_id || ''', ' ELSE 'NULL, NULL, ' END
--         || CASE WHEN expired_date IS NOT NULL THEN '''' || TO_CHAR(expired_date, 'YYYY-MM-DD') || ''', ''' || audit_user_id || '''' ELSE 'NULL, NULL' END
--         || '),'
--         AS values_sql
-- FROM REFERENCE_CODES
-- WHERE domain IN (
--     'CSIP_FAC',
--     'CSIP_FUNC',
--     'CSIP_INTVROL',
--     'CSIP_INV',
--     'CSIP_LOC',
--     'CSIP_OUT',
--     'CSIP_ROLE',
--     'CSIP_TYP'
-- )
-- ORDER BY domain, code;
-- And applying custom ordering

INSERT INTO reference_data
    (domain, code, description, list_sequence, created_at, created_by, last_modified_at, last_modified_by, deactivated_at, deactivated_by)
VALUES
    ('CONTRIBUTORY_FACTOR_TYPE', 'AFL', 'Alcohol or fermenting liquid', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'BAS', 'Basic needs (property or cell)', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'BUL', 'Bullying', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'DEB', 'Debt', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'FTC', 'First time in custody', 50, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'GAN', 'Gang related', 60, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'MED', 'Medication', 70, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'MEN', 'Mental health', 80, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'PIS', 'Psychoactive or illicit substance', 90, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'RAC', 'Racially motivated', 100, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'REL', 'Religiously motivated', 110, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'SEX', 'Sexually motivated', 120, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'SMO', 'Smoking withdrawal', 130, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'STA', 'Staff issues', 140, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('CONTRIBUTORY_FACTOR_TYPE', 'NRT', 'Vaporiser or NRT debt', 150, '2018-10-30 15:56:35', 'HQU53Y_CEN', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'ACT', 'Activities', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'CEN', 'Censors', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'CHA', 'Chaplaincy', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'CON', 'Control room', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'EDU', 'Education', 50, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'EST', 'Estates', 60, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'FIN', 'Finance', 70, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'GAR', 'Gardens or grounds', 80, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'GRO', 'Grounds', 90, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'GYM', 'Gymnasium', 100, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'HEA', 'Health care', 110, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'H', 'Health', 120, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'IMB', 'IMB', 130, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'KIT', 'Kitchen', 140, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'LIB', 'Library', 150, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'MEN', 'Mental health', 160, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'OFF', 'Offender management', 170, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'OPE', 'Operations', 180, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'OTH', 'Other', 190, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'PEO', 'People HUB', 200, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'PRO', 'Probation', 210, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'PRG', 'Programmes', 220, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'PSY', 'Psychology', 230, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'REC', 'Reception', 240, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'RES', 'Resettlement', 250, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'RSD', 'Residential', 260, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'SAF', 'Safety Team', 270, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'SEC', 'Security', 280, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'STO', 'Stores', 290, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'SUB', 'Substance Misuse Team', 300, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'VIS', 'Visits', 310, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('AREA_OF_WORK', 'WOR', 'Workshop', 320, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INTERVIEWEE_ROLE', 'PERP', 'Perpetrator', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INTERVIEWEE_ROLE', 'VICTIM', 'Victim', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INTERVIEWEE_ROLE', 'WITNESS', 'Witness', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INTERVIEWEE_ROLE', 'OTHER', 'Other', 40, '2018-10-30 15:59:28', 'HQU53Y_CEN', NULL, NULL, NULL, NULL),
    ('INCIDENT_INVOLVEMENT', 'PER', 'Perpetrator', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_INVOLVEMENT', 'VIC', 'Victim', 20, '2018-10-30 16:00:46', 'HQU53Y_CEN', NULL, NULL, NULL, NULL),
    ('INCIDENT_INVOLVEMENT', 'WIT', 'Witness', 30, '2018-10-30 16:00:46', 'HQU53Y_CEN', NULL, NULL, NULL, NULL),
    ('INCIDENT_INVOLVEMENT', 'OTH', 'Other', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'EDU', 'Education', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'EXY', 'Exercise yard', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'GRO', 'Grounds', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'GYM', 'Gym', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'HEA', 'Health care', 50, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'KIT', 'Kitchen', 60, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'LIB', 'Library', 70, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'MUF', 'Multi-faith', 80, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'NOI', 'No incident', 90, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'OMU', 'OMU', 100, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'OTH', 'Other', 110, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'REC', 'Reception', 120, '2018-10-27 18:11:30', 'OMS_OWNER', '2018-10-30 15:55:15', 'HQU53Y_CEN', NULL, NULL),
    ('INCIDENT_LOCATION', 'VIS', 'Visits', 130, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'WIC', 'Wing - cell', 140, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'WIL', 'Wing - landing', 150, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_LOCATION', 'WOR', 'Workshop', 160, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('OUTCOME_TYPE', 'CUR', 'Progress to CSIP', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('OUTCOME_TYPE', 'OPE', 'Progress to investigation', 20, '2018-10-27 18:11:30', 'OMS_OWNER', '2019-09-21 18:37:19', 'OMS_OWNER', NULL, NULL),
    ('OUTCOME_TYPE', 'WIN', 'Support outside of CSIP', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('OUTCOME_TYPE', 'ACC', 'ACCT supporting', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('OUTCOME_TYPE', 'NFA', 'No further action', 50, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('DECISION_SIGNER_ROLE', 'CUSTMAN', 'Custodial Manager', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('DECISION_SIGNER_ROLE', 'THEHOFF', 'Head of Function', 20, '2018-10-30 15:57:22', 'HQU53Y_CEN', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'ATO', 'Abuse or threats - other', 10, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'ATS', 'Abuse or threats - staff', 20, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'FTE', 'Failure to engage', 30, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'FEB', 'Frequent or escalating behaviour', 40, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'FPB', 'Frequent periods on Basic', 50, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'INT', 'Intimidation', 60, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'ISO', 'Isolation', 70, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'MDO', 'Multiple disciplinary offences', 80, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'OTH', 'Other', 90, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'PRS', 'Prisoner statement', 100, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'RPS', 'Repeat periods of segregation', 110, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'SEC', 'Security intelligence', 120, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'SEX', 'Sexual assault', 130, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'THR', 'Threats - staff', 140, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'UWI', 'Unwitnessed injury', 150, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'VIP', 'VIPER score', 160, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'VPA', 'Violence - prisoner assault', 170, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'VPF', 'Violence - prisoner fight', 180, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'VSA', 'Violence - staff assault', 190, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL),
    ('INCIDENT_TYPE', 'WIT', 'Witness', 200, '2018-10-27 18:11:30', 'OMS_OWNER', NULL, NULL, NULL, NULL);
