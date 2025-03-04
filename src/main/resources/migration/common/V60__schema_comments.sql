-- CSIP tables
COMMENT ON TABLE reference_data IS 'Reference data used to populate properties that have fixed lists of valid options e.g. categories, decisions and statuses';
COMMENT ON TABLE csip_record IS 'Root CSIP entity associating a person with a CSIP and all the child entities. Conceptually a CSIP folder. All 1:1 child entities use the record_id value for their primary key';
COMMENT ON TABLE person_summary IS 'Summary data for people with CSIPs. Used to optimise searching and pagination. Domain events cause the summary data to be updated keeping it accurate';
COMMENT ON TABLE referral IS 'The referral that caused the CSIP record to be created. Referrals are step 1 of the CSIP process';
COMMENT ON TABLE contributory_factor IS 'The contributory factors the referrer has identified as part of the referral';
COMMENT ON TABLE safer_custody_screening_outcome IS 'The result of the referral screening process undertaken by the Safer Custody team. Screening is step 2 of the CSIP process which can end if the decision is not to proceed further';
COMMENT ON TABLE investigation IS 'The investigation that took place following a ''Progress to investigation'' screening outcome. Investigations are an optional step 3 of the CSIP process';
COMMENT ON TABLE interview IS 'The interviews undertaken as part of the investigation';
COMMENT ON TABLE decision_and_actions IS 'Record of the decision and any actions expected as a result. Recording a decision is an optional step 4 of the CSIP process following an investigation';
COMMENT ON TABLE plan IS 'The plan that was developed following a screening outcome or decision to ''Progress to CSIP''. Developing a plan is step 5 of the CSIP process';
COMMENT ON TABLE identified_need IS 'The needs identified in the plan or at subsequent reviews. Includes the plan to resolve the identified need and the progression of that plan';
COMMENT ON TABLE review IS 'The reviews of the plan. Plans are reviewed regularly as step 6 of the CSIP process. Plans can close the CSIP completing the CSIP process';
COMMENT ON TABLE attendee IS 'Records the people who attended the review, their role and contribution';

--CSIP Hibernate Envers tables
COMMENT ON TABLE audit_revision IS 'Hibernate Envers audit revision records. A revision is created for every set of changes to the CSIP entity graph and the history of every mutable property change is tracked. Full audit history starts from the 15th of November 2024';
COMMENT ON TABLE csip_record_audit IS 'CSIP record property changes';
COMMENT ON TABLE person_summary_audit IS 'Person summary property changes';
COMMENT ON TABLE referral_audit IS 'Referral property changes';
COMMENT ON TABLE contributory_factor_audit IS 'Contributory factor property changes';
COMMENT ON TABLE safer_custody_screening_outcome_audit IS 'Screening property changes';
COMMENT ON TABLE investigation_audit IS 'Investigation property changes';
COMMENT ON TABLE interview_audit IS 'Interview property changes';
COMMENT ON TABLE decision_and_actions_audit IS 'Decision and actions property changes';
COMMENT ON TABLE plan_audit IS 'Plan property changes';
COMMENT ON TABLE identified_need_audit IS 'Identified needs property changes';
COMMENT ON TABLE review_audit IS 'Review property changes';
COMMENT ON TABLE attendee_audit IS 'Attendee property changes';

-- reference_data table
COMMENT ON COLUMN reference_data.reference_data_id IS 'Internal primary key. Not returned by API';
COMMENT ON COLUMN reference_data.domain IS 'The reference data domain. CSIP to NOMIS mappings: AREA_OF_WORK -> CSIP_FUNC, CONTRIBUTORY_FACTOR_TYPE -> CSIP_FAC, DECISION_OUTCOME_TYPE -> CSIP_OUT, DECISION_SIGNER_ROLE -> CSIP_ROLE, INCIDENT_INVOLVEMENT -> CSIP_INV, INCIDENT_LOCATION -> CSIP_LOC, INCIDENT_TYPE -> CSIP_TYP, INTERVIEWEE_ROLE -> CSIP_INTVROL, SCREENING_OUTCOME_TYPE -> CSIP_OUT, STATUS -> CSIP only domain';
COMMENT ON COLUMN reference_data.code IS 'The short code for the reference data item. Combination of domain and code is unique but individual codes are not guaranteed to be unique across domains. Matches the NOMIS reference data code for the associated domain';
COMMENT ON COLUMN reference_data.description IS 'The description of the reference data item';
COMMENT ON COLUMN reference_data.list_sequence IS 'Used to order reference data to guarantee the ordering for the UI';
COMMENT ON COLUMN reference_data.created_at IS 'The date and time the reference data item was created';
COMMENT ON COLUMN reference_data.created_by IS 'The username of the user who created the reference data item';
COMMENT ON COLUMN reference_data.last_modified_at IS 'The date and time the reference data item was last modified';
COMMENT ON COLUMN reference_data.last_modified_by IS 'The username of the user who last modified the reference data item';
COMMENT ON COLUMN reference_data.deactivated_at IS 'The date and time the reference data item was deactivated';
COMMENT ON COLUMN reference_data.deactivated_by IS 'The username of the user who deactivated the reference data item';

-- csip_record table
COMMENT ON COLUMN csip_record.record_id IS 'Public primary key for the CSIP and the shared primary key for 1:1 child entities';
COMMENT ON COLUMN csip_record.prison_number IS 'The prison number of the person the CSIP record is for';
COMMENT ON COLUMN csip_record.prison_code_when_recorded IS 'The prison code where the person was resident at the time the CSIP record was created. Maps to OFFENDER_CSIP_REPORTS.AGY_LOC_ID';
COMMENT ON COLUMN csip_record.log_code IS 'User entered identifier for the CSIP record. Usually starts with the prison code. Maps to OFFENDER_CSIP_REPORTS.CSIP_SEQ';
COMMENT ON COLUMN csip_record.status_id IS 'Foreign key to reference data using the ''STATUS'' domain for the overall CSIP status. Status is calculated in NOMIS and not stored in the database';
COMMENT ON COLUMN csip_record.legacy_id IS 'The NOMIS OFFENDER_CSIP_REPORTS.CSIP_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN csip_record.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- person_summary table
COMMENT ON COLUMN person_summary.prison_number IS 'The prison number of the person the summary data is associated with';
COMMENT ON COLUMN person_summary.first_name IS 'The first name of the person';
COMMENT ON COLUMN person_summary.last_name IS 'The last name of the person';
COMMENT ON COLUMN person_summary.status IS 'The active/inactive in/out status of the person';
COMMENT ON COLUMN person_summary.restricted_patient IS 'Whether the person is a restricted patient';
COMMENT ON COLUMN person_summary.supporting_prison_code IS 'The code of the prison a restricted patient is being supported by';
COMMENT ON COLUMN person_summary.prison_code IS 'The code of the prison the person is resident at or TRN/OUT if they are not in prison';
COMMENT ON COLUMN person_summary.cell_location IS 'The cell location of the person if they are currently in prison or an alternative location reference';
COMMENT ON COLUMN person_summary.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- referral table
COMMENT ON COLUMN referral.referral_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id';
COMMENT ON COLUMN referral.incident_date IS 'The date the incident that motivated the CSIP referral occurred. Will be displayed on the UI as date behaviour first occurred for a proactive referral Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_DATE';
COMMENT ON COLUMN referral.incident_time IS 'The time the incident that motivated the CSIP referral occurred. Will be displayed on the UI as time behaviour first occurred for a proactive referral. Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TIME';
COMMENT ON COLUMN referral.incident_type_id IS 'Foreign key to reference data using the ''INCIDENT_TYPE'' domain for the type of incident (or behavior for a proactive referral) that motivated the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TYPE';
COMMENT ON COLUMN referral.incident_location_id IS 'Foreign key to reference data using the ''INCIDENT_LOCATION'' domain for the location of the incident (or behavior for a proactive referral) that motivated the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_LOCATION';
COMMENT ON COLUMN referral.referred_by IS 'Either the display name of the user making the referral or free text if making the referral on behalf of someone else. Therefore not guaranteed to match user information. Maps to OFFENDER_CSIP_REPORTS.RFR_REPORTED_BY';
COMMENT ON COLUMN referral.referer_area_of_work_id IS 'Foreign key to reference data using the ''AREA_OF_WORK'' domain for the area of work of the person making the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_CSIP_FUNCTION';
COMMENT ON COLUMN referral.referral_date IS 'The date the referral was submitted. Set to today by the UI. Maps to OFFENDER_CSIP_REPORTS.RFR_DATE_REPORTED';
COMMENT ON COLUMN referral.proactive_referral IS 'Whether the referral was proactive or the result of an incident. Maps to OFFENDER_CSIP_REPORTS.RFR_PROACTIVE_RESPONSE where the default is ''N''';
COMMENT ON COLUMN referral.staff_assaulted IS 'Whether any member(s) of staff assaulted in the incident. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_ASSAULTED where the default is ''N''';
COMMENT ON COLUMN referral.assaulted_staff_name IS 'Names of any assaulted member(s) of staff. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_NAME which has a 1000 character limit';
COMMENT ON COLUMN referral.incident_involvement_id IS 'Foreign key to reference data using the ''INCIDENT_INVOLVEMENT'' domain for the type of involvement the person had in the incident (or behavior for a proactive referral). The reference_data.code value maps to OFFENDER_CSIP_REPORTS.CDR_INVOLVEMENT';
COMMENT ON COLUMN referral.description_of_concern IS 'The reasons why there is cause for concern. Maps to OFFENDER_CSIP_REPORTS.CDR_CONCERN_DESCRIPTION which has a 4000 character limit';
COMMENT ON COLUMN referral.known_reasons IS 'The reasons already known about the causes of the incident or behaviour. Maps to OFFENDER_CSIP_REPORTS.INV_KNOWN_REASONS which has a 4000 character limit';
COMMENT ON COLUMN referral.other_information IS 'Any other information about the incident or behaviour. Maps to OFFENDER_CSIP_REPORTS.CDR_OTHER_INFORMATION which has a 4000 character limit';
COMMENT ON COLUMN referral.safer_custody_team_informed IS 'Records whether the safer custody team been informed. Maps to OFFENDER_CSIP_REPORTS.CDR_SENT_DENT where the default is ''N''';
COMMENT ON COLUMN referral.referral_complete IS 'Whether the referral complete. Will be true when DPS is used to make a referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETE_FLAG where the default is ''N''';
COMMENT ON COLUMN referral.referral_completed_by IS 'The username of the user who completed the referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_BY';
COMMENT ON COLUMN referral.referral_completed_by_display_name IS 'The first and last name of the user who completed the referral. Does not update if their name changes';
COMMENT ON COLUMN referral.referral_completed_date IS 'The date the referral was completed. Will be set to today when DPS is used to make a referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_DATE';
COMMENT ON COLUMN referral.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- contributory_factor table
COMMENT ON COLUMN contributory_factor.contributory_factor_id IS 'Public primary key';
COMMENT ON COLUMN contributory_factor.referral_id IS 'Parent referral foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id';
COMMENT ON COLUMN contributory_factor.contributory_factor_type_id IS 'Foreign key to reference data using the ''CONTRIBUTORY_FACTOR_TYPE'' domain for the type of contributory factor believed to have caused the incident or behaviour. The reference_data.code value maps to OFFENDER_CSIP_FACTORS.CSIP_FACTOR';
COMMENT ON COLUMN contributory_factor.comment IS 'Additional information about the contributory factor. Maps to OFFENDER_CSIP_FACTORS.COMMENTS which has a 4000 character limit';
COMMENT ON COLUMN contributory_factor.legacy_id IS 'The NOMIS OFFENDER_CSIP_FACTORS.CSIP_FACTOR_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN contributory_factor.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- safer_custody_screening_outcome table
COMMENT ON COLUMN safer_custody_screening_outcome.safer_custody_screening_outcome_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id';
COMMENT ON COLUMN safer_custody_screening_outcome.outcome_id IS 'Foreign key to reference data using the ''SCREENING_OUTCOME_TYPE'' domain for the selected screening outcome. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by IS 'The username of the user who recorded the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_RECORDED_BY';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by_display_name IS 'The first and last name of the user who recorded the safer custody screening outcome. Does not update if their name changes';
COMMENT ON COLUMN safer_custody_screening_outcome.date IS 'The date of the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_DATE';
COMMENT ON COLUMN safer_custody_screening_outcome.reason_for_decision IS 'The reasons for the safer custody screening outcome decision. Maps to OFFENDER_CSIP_REPORTS.CDR_DECISION_REASON which has a 4000 character limit';
COMMENT ON COLUMN safer_custody_screening_outcome.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- investigation table
COMMENT ON COLUMN investigation.investigation_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id';
COMMENT ON COLUMN investigation.staff_involved IS 'The names of the staff involved in the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_STAFF_INVOLVED which has a 4000 character limit';
COMMENT ON COLUMN investigation.evidence_secured IS 'Any evidence that was secured as part of the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_EVIDENCE_SECURED which has a 4000 character limit';
COMMENT ON COLUMN investigation.occurrence_reason IS 'The reasons why the incident or behaviour occurred. Maps to OFFENDER_CSIP_REPORTS.INV_OCCURRENCE_REASON which has a 4000 character limit';
COMMENT ON COLUMN investigation.persons_usual_behaviour IS 'The normal behaviour of the person in prison. Maps to OFFENDER_CSIP_REPORTS.INV_USUAL_BEHAVIOUR which has a 4000 character limit';
COMMENT ON COLUMN investigation.persons_trigger IS 'What triggers the person in prison has that could have motivated the incident. Maps to OFFENDER_CSIP_REPORTS.INV_PERSONS_TRIGGER which has a 4000 character limit';
COMMENT ON COLUMN investigation.protective_factors IS 'Any protective factors. Maps to OFFENDER_CSIP_REPORTS.INV_PROTECTIVE_FACTORS which has a 4000 character limit';
COMMENT ON COLUMN investigation.recorded_by IS 'The username of the user who recorded the investigation. New field populated from 13th of February 2025 and only via DPS';
COMMENT ON COLUMN investigation.recorded_by_display_name IS 'The first and last name of the user who recorded the investigation. New field populated from 13th of February 2025 and only via DPS. Does not update if their name changes';
COMMENT ON COLUMN investigation.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- interview table
COMMENT ON COLUMN interview.interview_id IS 'Public primary key';
COMMENT ON COLUMN interview.investigation_id IS 'Parent investigation foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id';
COMMENT ON COLUMN interview.interviewee IS 'Name of the person being interviewed. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_INTVW.CSIP_INTERVIEWEE which has a 100 character limit';
COMMENT ON COLUMN interview.interview_date IS 'The date the interview took place. Maps to OFFENDER_CSIP_INTVW.INTVW_DATE';
COMMENT ON COLUMN interview.interviewee_role_id IS 'Foreign key to reference data using the ''INTERVIEWEE_ROLE'' domain for the role the interviewee played in the incident or behaviour. Maps to OFFENDER_CSIP_INTVW.INTVW_ROLE';
COMMENT ON COLUMN interview.interview_text IS 'Information provided in interview. Maps to OFFENDER_CSIP_INTVW.COMMENTS which has a 4000 character limit';
COMMENT ON COLUMN interview.legacy_id IS 'The NOMIS OFFENDER_CSIP_INTVW.CSIP_INTVW_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN interview.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- decision_and_actions table
COMMENT ON COLUMN decision_and_actions.decision_and_actions_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id';
COMMENT ON COLUMN decision_and_actions.conclusion IS 'The conclusion and reasons for the decision. Maps to OFFENDER_CSIP_REPORTS.INV_CONCLUSION which has a 4000 character limit';
COMMENT ON COLUMN decision_and_actions.outcome_id IS 'Foreign key to reference data using the ''DECISION_OUTCOME_TYPE'' domain for the selected decision. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME';
COMMENT ON COLUMN decision_and_actions.signed_off_by_role_id IS 'Foreign key to reference data using the ''DECISION_SIGNER_ROLE'' domain for the role of the person who signed off the decision. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.INV_SIGNED_OFF_BY';
COMMENT ON COLUMN decision_and_actions.recorded_by IS 'The username of the user who recorded the decision. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_RECORDED_BY';
COMMENT ON COLUMN decision_and_actions.recorded_by_display_name IS 'The first and last name of the user who recorded the decision. Does not update if their name changes';
COMMENT ON COLUMN decision_and_actions.date IS 'The date the decision was made. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_DATE';
COMMENT ON COLUMN decision_and_actions.next_steps IS 'The next steps that should be taken following the decision. Maps to OFFENDER_CSIP_REPORTS.INV_NEXT_STEPS which has a 4000 character limit';
COMMENT ON COLUMN decision_and_actions.actions IS 'An enumeration of actions that have been recommended. CSIP to NOMIS mappings: ''OPEN_CSIP_ALERT'' -> OFFENDER_CSIP_REPORTS.OPEN_CSIP_ALERT, ''NON_ASSOCIATIONS_UPDATED'' -> OFFENDER_CSIP_REPORTS.INV_NON_ASSOC_BOOKED, ''OBSERVATION_BOOK'' -> OFFENDER_CSIP_REPORTS.INV_OBSERVATION_BOOK, ''UNIT_OR_CELL_MOVE'' -> OFFENDER_CSIP_REPORTS.INV_MOVE, ''CSRA_OR_RSRA_REVIEW'' -> OFFENDER_CSIP_REPORTS.INV_REVIEW, ''SERVICE_REFERRAL'' -> OFFENDER_CSIP_REPORTS.INV_SERVICE_REFERRAL, ''SIM_REFERRAL'' -> OFFENDER_CSIP_REPORTS.INV_SIM_REFERRAL. Only populated via NOMIS and retained for sync and reporting';
COMMENT ON COLUMN decision_and_actions.action_other IS 'Any other actions that are recommended to be considered. Maps to OFFENDER_CSIP_REPORTS.INV_OTHER which has a 4000 character limit';
COMMENT ON COLUMN decision_and_actions.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- plan table
COMMENT ON COLUMN plan.plan_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id';
COMMENT ON COLUMN plan.case_manager IS 'The case manager assigned to the CSIP plan. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_REPORTS.CASE_MANAGER which has a 100 character limit';
COMMENT ON COLUMN plan.reason_for_plan IS 'The reasons motivating the creation of a CSIP plan. Maps to OFFENDER_CSIP_REPORTS.REASON which has a 240 character limit';
COMMENT ON COLUMN plan.first_case_review_date IS 'The first date the CSIP plan should be reviewed. Maps to OFFENDER_CSIP_REPORTS.CASE_REV_DATE';
COMMENT ON COLUMN plan.next_review_date IS 'The next date the plan should be reviewed. Set as the review.review_date value from the latest review. On the plan entity for query optimisation';
COMMENT ON COLUMN plan.closed_date IS 'The date the plan was closed. Set as the review.csip_closed_date value from the first review containing an action to close the CSIP. On the plan entity for query optimisation';
COMMENT ON COLUMN plan.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- identified_need table
COMMENT ON COLUMN identified_need.identified_need_id IS 'Public primary key';
COMMENT ON COLUMN identified_need.plan_id IS 'Parent plan foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id';
COMMENT ON COLUMN identified_need.identified_need IS 'Details of the need identified in the CSIP plan. Maps to OFFENDER_CSIP_PLANS.IDENTIFIED_NEED which has a 1000 character limit';
COMMENT ON COLUMN identified_need.responsible_person IS 'The name of the person who is responsible for taking action on the intervention. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_PLANS.BY_WHOM which has a 100 character limit';
COMMENT ON COLUMN identified_need.created_date IS 'The date the need was identified. Maps to OFFENDER_CSIP_PLANS.CREATE_DATE';
COMMENT ON COLUMN identified_need.target_date IS 'The target date the need should be progressed or resolved. Maps to OFFENDER_CSIP_PLANS.TARGET_DATE';
COMMENT ON COLUMN identified_need.closed_date IS 'The date the identified need was resolved and closed. Maps to OFFENDER_CSIP_PLANS.CLOSED_DATE';
COMMENT ON COLUMN identified_need.intervention IS 'The planned intervention for the identified need. Maps to OFFENDER_CSIP_PLANS.INTERVENTION which has a 4000 character limit';
COMMENT ON COLUMN identified_need.progression IS 'How the plan to address the identified need is progressing. Maps to OFFENDER_CSIP_PLANS.PROGRESSION which has a 4000 character limit';
COMMENT ON COLUMN identified_need.legacy_id IS 'The NOMIS OFFENDER_CSIP_PLANS.PLAN_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN identified_need.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- review table
COMMENT ON COLUMN review.review_id IS 'Public primary key';
COMMENT ON COLUMN review.plan_id IS 'Parent plan foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id';
COMMENT ON COLUMN review.review_sequence IS 'The review sequence which starts at 1 and increments for each new review. Used to order the reviews. Maps to OFFENDER_CSIP_REVIEWS.REVIEW_SEQ';
COMMENT ON COLUMN review.review_date IS 'The date of the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_DATE';
COMMENT ON COLUMN review.recorded_by IS 'The username of the person who recorded the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_USER';
COMMENT ON COLUMN review.recorded_by_display_name IS 'The first and last name of the user who recorded the review. Does not update if their name changes';
COMMENT ON COLUMN review.next_review_date IS 'The date of the next review. Maps to OFFENDER_CSIP_REVIEWS.NEXT_REVIEW_DATE';
COMMENT ON COLUMN review.actions IS 'An enumeration of actions that should be taken following the review one of which is to close the CSIP. CSIP to NOMIS mappings: ''CLOSE_CSIP'' -> OFFENDER_CSIP_REVIEWS.CLOSE_CSIP, ''REMAIN_ON_CSIP'' -> OFFENDER_CSIP_REVIEWS.REMAIN_ON_CSIP, ''RESPONSIBLE_PEOPLE_INFORMED'' -> OFFENDER_CSIP_REVIEWS.PEOPLE_INFORMED, ''CSIP_UPDATED'' -> OFFENDER_CSIP_REVIEWS.CSIP_UPDATED, ''CASE_NOTE'' -> OFFENDER_CSIP_REVIEWS.CASE_NOTE. Only CLOSE_CSIP is populated via DPS. The rest are populated via NOMIS only and retained for sync and reporting';
COMMENT ON COLUMN review.csip_closed_date IS 'The date the CSIP plan was closed following a review outcome decision to close it. Maps to OFFENDER_CSIP_REVIEWS.CLOSE_DATE';
COMMENT ON COLUMN review.summary IS 'Additional information about the review. Maps to OFFENDER_CSIP_REVIEWS.SUMMARY which has a 4000 character limit';
COMMENT ON COLUMN review.legacy_id IS 'The NOMIS OFFENDER_CSIP_REVIEWS.REVIEW_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN review.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- attendee table
COMMENT ON COLUMN attendee.attendee_id IS 'Public primary key';
COMMENT ON COLUMN attendee.review_id IS 'Parent review foreign key';
COMMENT ON COLUMN attendee.name IS 'Name of review attendee/contributor. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_NAME which has a 100 character limit';
COMMENT ON COLUMN attendee.role IS 'Role of review attendee/contributor. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_ROLE which has a 50 character limit';
COMMENT ON COLUMN attendee.attended IS 'Whether the person attended the review. Indicates that they were a contributor if false. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDED where the default is ''N''';
COMMENT ON COLUMN attendee.contribution IS 'Description of attendee contribution. Maps to OFFENDER_CSIP_ATTENDEES.CONTRIBUTION which has a 4000 character limit';
COMMENT ON COLUMN attendee.legacy_id IS 'The NOMIS OFFENDER_CSIP_ATTENDEES.ATTENDEE_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API';
COMMENT ON COLUMN attendee.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities';

-- audit_revision table
COMMENT ON COLUMN audit_revision.id IS 'Internal primary key';
COMMENT ON COLUMN audit_revision.timestamp IS 'The date and time the change happened';
COMMENT ON COLUMN audit_revision.username IS 'The username of the user who made the change';
COMMENT ON COLUMN audit_revision.caseload_id IS 'The active caseload of the user at the time of the change';
COMMENT ON COLUMN audit_revision.source IS 'The system used to make the change. Either DPS or NOMIS';
COMMENT ON COLUMN audit_revision.affected_components IS 'The table or tables that had values affected by the change';

-- _audit tables
COMMENT ON COLUMN csip_record_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN person_summary_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN referral_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN contributory_factor_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN investigation_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN interview_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN decision_and_actions_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN plan_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN identified_need_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN review_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';
COMMENT ON COLUMN attendee_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted';