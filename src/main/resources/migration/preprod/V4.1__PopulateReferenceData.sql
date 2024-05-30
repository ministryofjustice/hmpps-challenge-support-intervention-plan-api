-- Import inactive reference data
INSERT INTO reference_data (domain, code, description, list_sequence, created_at, created_by, deactivated_at,
                            deactivated_by)
VALUES ('OUTCOME_TYPE', 'ACC', 'ACCT Supporting', 99, '2024-05-28 00:00:00', 'IMPORTED', '2024-05-28 00:00:00',
        'IMPORTED');

-- Import active reference data
INSERT INTO reference_data (domain, code, description, list_sequence, created_at, created_by)
VALUES ('CONTRIBUTORY_FACTOR_TYPE', 'AFL', 'Alcohol/Fermenting Liquid', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'BAS', 'Basic Needs (Property/Cell)', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'BUL', 'Bullying', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'DEB', 'Debt', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'FTC', 'First Time in Custody', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'GAN', 'Gang Related', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'MED', 'Medication', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'MEN', 'Mental Heath', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'NRT', 'Vaporiser/NRT Debt', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'PIS', 'Pyschoactive/Illicit Substance ', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'RAC', 'Racially Motivated', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'REL', 'Religiously Motivated', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'SEX', 'Sexually Motivated', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'SMO', 'Smoking Withdrawal', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('CONTRIBUTORY_FACTOR_TYPE', 'STA', 'Staff Issues', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'ACT', 'Activities', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'CEN', 'Censors', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'CHA', 'Chaplaincy', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'CON', 'Control Room', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'EDU', 'Education', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'EST', 'Estates', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'FIN', 'Finance', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'GAR', 'Gardens\\Grounds', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'GRO', 'Grounds', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'GYM', 'Gymnasium', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'H', 'Health ', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'HEA', 'Health Care', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'IMB', 'IMB', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'KIT', 'Kitchen', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'LIB', 'Library', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'MEN', 'Mental Health', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'OFF', 'Offender Management', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'OPE', 'Operations', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'OTH', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'PEO', 'People HUB', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'PRG', 'Programmes', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'PRO', 'Probation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'PSY', 'Psychology', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'REC', 'Reception', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'RES', 'Resettlement', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'RSD', 'Residential', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'SAF', 'Safety Team', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'SEC', 'Security', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'STO', 'Stores', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'SUB', 'Substance Mis-Use Team', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'VIS', 'Visits', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('AREA_OF_WORK', 'WOR', 'Workshop', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'OTHER', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'PERP', 'Perpetrator', 10, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'VICTIM', 'Victim', 20, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INTERVIEWEE_ROLE', 'WITNESS', 'Witness', 30, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_INVOLVEMENT', 'OTH', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_INVOLVEMENT', 'PER', 'Perpertrator', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_INVOLVEMENT', 'VIC', 'Victim', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_INVOLVEMENT', 'WIT', 'Witness', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'EDU', 'Education', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'EXY', 'Exercise Yard', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'GRO', 'Grounds', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'GYM', 'Gym', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'HEA', 'Health Care', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'KIT', 'Kitchen', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'LIB', 'Library', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'MUF', 'Multi-Faith', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'NOI', 'No Incident', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'OMU', 'OMU', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'OTH', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'REC', 'Reception', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'VIS', 'Visits', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'WIC', 'Wing - Cell', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'WIL', 'Wing - Landing', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_LOCATION', 'WOR', 'Workshop', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'CUR', 'Progress to CSIP', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'NFA', 'No Further Action', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'OPE', 'Progress to Investigation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('OUTCOME_TYPE', 'WIN', 'Support to be provided outside of CSIP', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('DECISION_SIGNER_ROLE', 'CUSTMAN', 'Custodial Manager', 10, '2024-05-28 00:00:00', 'IMPORTED'),
       ('DECISION_SIGNER_ROLE', 'THEHOFF', 'Head of Function', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'ATO', 'Abuse/Threats Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'ATS', 'Abuse/Threats Staff', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'FEB', 'Frequent/Escalating Behaviour', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'FPB', 'Frequent periods on Basic', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'FTE', 'Failure to Engage', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'INT', 'Intimidation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'ISO', 'Isolation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'MDO', 'Multiple Disciplinary Offences', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'OTH', 'Other', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'PRS', 'Prisoner Statement', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'RPS', 'Repeat periods of Segregation', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'SEC', 'Security Intelligence', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'SEX', 'Sexual Assault', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'THR', 'Threats Staff', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'UWI', 'Unwitnessed Injury', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'VIP', 'VIPER Score', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'VPA', 'Violence - Prisoner Assault', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'VPF', 'Violence - Prisoner Fight', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'VSA', 'Violence - Staff Assault', 99, '2024-05-28 00:00:00', 'IMPORTED'),
       ('INCIDENT_TYPE', 'WIT', 'Witness', 99, '2024-05-28 00:00:00', 'IMPORTED');
