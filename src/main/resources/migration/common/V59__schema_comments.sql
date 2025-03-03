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

-- csip_record table
COMMENT ON COLUMN csip_record.record_id IS 'not null unique. Mapped to OFFENDER_CSIP_REPORTS';
COMMENT ON COLUMN csip_record.legacy_id IS 'unique. Prevents duplicate records via NOMIS -> DPS sync';
COMMENT ON COLUMN csip_record.prison_number IS 'not null. The prison number of the person the CSIP record is for';
COMMENT ON COLUMN csip_record.prison_code_when_recorded IS 'The prison code where the person was resident at the time the CSIP record was created. Maps to OFFENDER_CSIP_REPORTS.AGY_LOC_ID';
COMMENT ON COLUMN csip_record.log_code IS 'User entered identifier for the CSIP record. Defaults to the prison code. Maps to OFFENDER_CSIP_REPORTS.CSIP_SEQ';
COMMENT ON COLUMN csip_record.version IS 'increments on each update';

-- referral table
COMMENT ON COLUMN referral.referral_id IS 'not null. Maps csip_record.record_id';
COMMENT ON COLUMN referral.incident_date IS 'not null. The date the incident that motivated the CSIP referral occurred. Maps to RFR_INCIDENT_DATE';
COMMENT ON COLUMN referral.incident_time IS 'The time the incident that motivated the CSIP referral occurred. Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TIME';
COMMENT ON COLUMN referral.incident_type_id IS 'not null. FK to reference_data. The type of incident that motivated the CSIP referral. Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TYPE';
COMMENT ON COLUMN referral.incident_location_id IS 'not null. FK to reference_data. The location of the incident that motivated the CSIP referral. Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_LOCATION';
COMMENT ON COLUMN referral.referred_by IS 'not null. The person reporting the incident or creating the CSIP referral. Maps to OFFENDER_CSIP_REPORTS.RFR_REPORTED_BY';
COMMENT ON COLUMN referral.referer_area_of_work_id IS 'not null. FK to reference_data. The area of work of the person reporting the incident or creating the CSIP referral. Maps to OFFENDER_CSIP_REPORTS.RFR_CSIP_FUNCTION';
COMMENT ON COLUMN referral.referral_date IS 'not null. The date the CSIP referral was raised. Maps to OFFENDER_CSIP_REPORTS.RFR_DATE_REPORTED';
COMMENT ON COLUMN referral.proactive_referral IS 'Was this referral proactive or preventative. Maps to OFFENDER_CSIP_REPORTS.RFR_PROACTIVE_RESPONSE where the default is ''N''';
COMMENT ON COLUMN referral.staff_assaulted IS 'Were any members of staff assaulted in the incident. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_ASSAULTED where the default is ''N''';
COMMENT ON COLUMN referral.assaulted_staff_name IS 'Name or names of assaulted members of staff if any. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_NAME. 1000 character limit';
COMMENT ON COLUMN referral.release_date IS 'The release date of the person in prison. Can be planned released date or the date they were released. Maps to OFFENDER_CSIP_REPORTS.CDR_RELEASE_DATE. Likely set by another screen in NOMIS';
COMMENT ON COLUMN referral.incident_involvement_id IS 'not null. FK to reference_data. The type of involvement the person had in the incident. Maps to OFFENDER_CSIP_REPORTS.CDR_INVOLVEMENT';
COMMENT ON COLUMN referral.description_of_concern IS 'not null. The reasons why there is cause for concern. Maps to OFFENDER_CSIP_REPORTS.CDR_CONCERN_DESCRIPTION';
COMMENT ON COLUMN referral.known_reasons IS 'not null. The reasons already known about the causes of the incident or motivation for CSIP referral. Maps to OFFENDER_CSIP_REPORTS.INV_KNOWN_REASONS';
COMMENT ON COLUMN referral.other_information IS 'Any other information about the incident or reasons for CSIP referral. Maps to OFFENDER_CSIP_REPORTS.CDR_OTHER_INFORMATION';
COMMENT ON COLUMN referral.safer_custody_team_informed IS 'Records whether the safer custody team been informed. Maps to OFFENDER_CSIP_REPORTS.CDR_SENT_DENT where the default is ''N''';
COMMENT ON COLUMN referral.referral_complete IS 'Is the referral complete. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETE_FLAG where the default is ''N''';
COMMENT ON COLUMN referral.referral_completed_by IS 'The username of the user who completed the CSIP referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_BY';
COMMENT ON COLUMN referral.referral_completed_by_display_name IS 'The displayable name of the user who completed the CSIP referral. Not mapped';
COMMENT ON COLUMN referral.referral_completed_date IS 'The date the referral was completed. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_DATE';
COMMENT ON COLUMN referral.version IS 'increments on each update';

-- contributory_factor table
COMMENT ON COLUMN contributory_factor.contributory_factor_id IS 'not null unique. Mapped to OFFENDER_CSIP_FACTORS';
COMMENT ON COLUMN contributory_factor.legacy_id IS 'unique. Prevents duplicate contributory factors via NOMIS -> DPS sync';
COMMENT ON COLUMN contributory_factor.referral_id IS 'not null';
COMMENT ON COLUMN contributory_factor.contributory_factor_type_id IS 'not null. FK to reference_data. The type of contributory factor to the incident or motivation for CSIP referral. Mapped to OFFENDER_CSIP_FACTORS.CSIP_FACTOR';
COMMENT ON COLUMN contributory_factor.comment IS 'Additional information about the contributory factor to the incident or motivation for CSIP referral. Maps to OFFENDER_CSIP_FACTORS.COMMENTS';
COMMENT ON COLUMN contributory_factor.version IS 'increments on each update';

-- safer_custody_screening_outcome table
COMMENT ON COLUMN safer_custody_screening_outcome.safer_custody_screening_outcome_id IS 'not null. Maps referral.referral_id';
COMMENT ON COLUMN safer_custody_screening_outcome.outcome_id IS 'not null. FK to reference_data. The type of outcome of the safer custody screening. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by IS 'not null. The username of the user who recorded the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_RECORDED_BY';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by_display_name IS 'not null. The displayable name of the user who recorded the safer custody screening outcome. Not mapped';
COMMENT ON COLUMN safer_custody_screening_outcome.date IS 'not null. The date of the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_DATE';
COMMENT ON COLUMN safer_custody_screening_outcome.reason_for_decision IS 'not null. The reasons for the safer custody screening outcome decision. Maps to OFFENDER_CSIP_REPORTS.CDR_DECISION_REASON. 4000 character limit';
COMMENT ON COLUMN safer_custody_screening_outcome.version IS 'increments on each update';

-- investigation table
COMMENT ON COLUMN investigation.investigation_id IS 'not null. Maps referral.referral_id';
COMMENT ON COLUMN investigation.staff_involved IS 'The names of the staff involved in the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_STAFF_INVOLVED. 4000 character limit';
COMMENT ON COLUMN investigation.evidence_secured IS 'Any evidence that was secured as part of the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_EVIDENCE_SECURED. 4000 character limit';
COMMENT ON COLUMN investigation.occurrence_reason IS 'The reasons why the incident occurred. Maps to OFFENDER_CSIP_REPORTS.INV_OCCURRENCE_REASON. 4000 character limit';
COMMENT ON COLUMN investigation.persons_usual_behaviour IS 'The normal behaviour of the person in prison. Maps to OFFENDER_CSIP_REPORTS.INV_USUAL_BEHAVIOUR. 4000 character limit';
COMMENT ON COLUMN investigation.persons_trigger IS 'What triggers the person in prison has that could have motivated the incident. Maps to OFFENDER_CSIP_REPORTS.INV_PERSONS_TRIGGER. 4000 character limit';
COMMENT ON COLUMN investigation.protective_factors IS 'Any protective factors. Maps to OFFENDER_CSIP_REPORTS.INV_PROTECTIVE_FACTORS. 4000 character limit';
COMMENT ON COLUMN investigation.version IS 'increments on each update';
recordedBy
recordedByDisplayName

-- interview table
COMMENT ON COLUMN interview.interview_id IS 'not null unique. Mapped to OFFENDER_CSIP_INTVW';
COMMENT ON COLUMN interview.legacy_id IS 'unique. Prevents duplicate interviews via NOMIS -> DPS sync';
COMMENT ON COLUMN interview.investigation_id IS 'not null';
COMMENT ON COLUMN interview.interviewee IS 'not null. Name of the person being interviewed. Maps to OFFENDER_CSIP_INTVW.CSIP_INTERVIEWEE';
COMMENT ON COLUMN interview.interview_date IS 'not null. The date the interview took place. Maps to OFFENDER_CSIP_INTVW.INTVW_DATE';
COMMENT ON COLUMN interview.interviewee_role_id IS 'not null. FK to reference_data. What role the interviewee played in the incident or referral. Maps to OFFENDER_CSIP_INTVW.INTVW_ROLE';
COMMENT ON COLUMN interview.interview_text IS 'Information provided in interview. Maps to OFFENDER_CSIP_INTVW.COMMENTS. 4000 character limit';
COMMENT ON COLUMN interview.version IS 'increments on each update';

-- decision_and_actions table
COMMENT ON COLUMN decision_and_actions.decision_and_actions_id IS 'not null. Maps referral.referral_id';
COMMENT ON COLUMN decision_and_actions.conclusion IS 'The conclusion of the referral and reasons for the outcome decision. Maps to OFFENDER_CSIP_REPORTS.INV_CONCLUSION. 4000 character limit';
COMMENT ON COLUMN decision_and_actions.outcome_id IS 'not null. FK to reference_data. The outcome decision for the referral. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME';
COMMENT ON COLUMN decision_and_actions.signed_off_by_role_id IS 'FK to reference_data. The role of the person making the outcome decision. Maps to OFFENDER_CSIP_REPORTS.INV_SIGNED_OFF_BY';
COMMENT ON COLUMN decision_and_actions.recorded_by IS 'The username of the user who recorded the outcome decision. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_RECORDED_BY';
COMMENT ON COLUMN decision_and_actions.recorded_by_display_name IS 'The displayable name of the user who recorded the outcome decision. Not mapped';
COMMENT ON COLUMN decision_and_actions.date IS 'The date the outcome decision was made. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_DATE';
COMMENT ON COLUMN decision_and_actions.next_steps IS 'The next steps that should be taken following the outcome decision. Maps to OFFENDER_CSIP_REPORTS.INV_NEXT_STEPS. 4000 character limit';
COMMENT ON COLUMN decision_and_actions.actions IS 'An enumeration of actions that may have been recommended [''OPEN_CSIP_ALERT'', ''NON_ASSOCIATIONS_UPDATED'', ''OBSERVATION_BOOK'', ''UNIT_OR_CELL_MOVE'', ''CSRA_OR_RSRA_REVIEW'', ''SERVICE_REFERRAL'', ''SIM_REFERRAL'']';
COMMENT ON COLUMN decision_and_actions.action_other IS 'Any other actions that are recommended to be considered. Maps to OFFENDER_CSIP_REPORTS.INV_OTHER. 4000 character limit';
COMMENT ON COLUMN decision_and_actions.version IS 'increments on each update';

-- plan table
COMMENT ON COLUMN plan.plan_id IS 'not null. Maps csip_record.record_id';
COMMENT ON COLUMN plan.case_manager IS 'not null. The case manager assigned to the CSIP plan. Maps to OFFENDER_CSIP_REPORTS.CASE_MANAGER';
COMMENT ON COLUMN plan.reason_for_plan IS 'not null. The reasons motivating the creation of a CSIP plan. Maps to OFFENDER_CSIP_REPORTS.REASON';
COMMENT ON COLUMN plan.first_case_review_date IS 'not null. The first date the CSIP plan should be reviewed. Maps to OFFENDER_CSIP_REPORTS.CASE_REV_DATE';
COMMENT ON COLUMN plan.version IS 'increments on each update';

-- identified_need table
COMMENT ON COLUMN identified_need.identified_need_id IS 'not null unique. Mapped to OFFENDER_CSIP_PLANS';
COMMENT ON COLUMN identified_need.legacy_id IS 'unique. Prevents duplicate identified needs via NOMIS -> DPS sync';
COMMENT ON COLUMN identified_need.plan_id IS 'not null';
COMMENT ON COLUMN identified_need.identified_need IS 'not null. Details of the need identified in the CSIP plan. Maps to OFFENDER_CSIP_PLANS.IDENTIFIED_NEED. 1000 character limit';
COMMENT ON COLUMN identified_need.responsible_person IS 'not null. The name of the person who is responsible for taking action on the intervention. Maps to OFFENDER_CSIP_PLANS.BY_WHOM. Who identified the need (free text)';
COMMENT ON COLUMN identified_need.created_date IS 'not null. The date the need was identified. Maps to OFFENDER_CSIP_PLANS.CREATE_DATE. Date the need was identified';
COMMENT ON COLUMN identified_need.target_date IS 'not null. The target date the need should be progressed or resolved. Maps to OFFENDER_CSIP_PLANS.TARGET_DATE. Target date of the identified need';
COMMENT ON COLUMN identified_need.closed_date IS 'The date the identified need was resolved or closed. Maps to OFFENDER_CSIP_PLANS.CLOSED_DATE';
COMMENT ON COLUMN identified_need.intervention IS 'not null. The planned intervention for the identified need. Maps to OFFENDER_CSIP_PLANS.INTERVENTION. 4000 character limit';
COMMENT ON COLUMN identified_need.progression IS 'How the plan to address the identified need. is progressing. Maps to OFFENDER_CSIP_PLANS.PROGRESSION. 4000 character limit';
COMMENT ON COLUMN identified_need.version IS 'increments on each update';

-- review table
COMMENT ON COLUMN review.review_id IS 'not null unique. Mapped to OFFENDER_CSIP_REVIEWS';
COMMENT ON COLUMN review.legacy_id IS 'unique. Prevents duplicate reviews via NOMIS -> DPS sync';
COMMENT ON COLUMN review.plan_id IS 'not null';
COMMENT ON COLUMN review.review_sequence IS 'not null. The review number. Maps to OFFENDER_CSIP_REVIEWS.REVIEW_SEQ';
COMMENT ON COLUMN review.review_date IS 'The date of the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_DATE';
COMMENT ON COLUMN review.recorded_by IS 'not null. The username of the person who recorded the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_USER';
COMMENT ON COLUMN review.recorded_by_display_name IS 'not null. The displayable name of the person who recorded the review. Not mapped';
COMMENT ON COLUMN review.next_review_date IS 'The date of the next review. Maps to OFFENDER_CSIP_REVIEWS.NEXT_REVIEW_DATE';
COMMENT ON COLUMN review.actions IS 'If the actions following the review include: informing people responsible for the person in prison, updating the CSIP plan, deciding the person should remain on the CSIP plan, adding a CSIP case note, and closing the CSIP plan. Maps to OFFENDER_CSIP_REVIEWS.PEOPLE_INFORMED, OFFENDER_CSIP_REVIEWS.CSIP_UPDATED, OFFENDER_CSIP_REVIEWS.REMAIN_ON_CSIP, OFFENDER_CSIP_REVIEWS.CASE_NOTE, and OFFENDER_CSIP_REVIEWS.CLOSE_CSIP, which all have default value ''N''';
COMMENT ON COLUMN review.csip_closed_date IS 'The date the CSIP plan was closed following a review outcome decision to close it. Maps to OFFENDER_CSIP_REVIEWS.CLOSE_DATE';
COMMENT ON COLUMN review.summary IS 'Additional information about the review. Maps to OFFENDER_CSIP_REVIEWS.SUMMARY. 4000 character limit';
COMMENT ON COLUMN review.version IS 'increments on each update';

-- attendee table
COMMENT ON COLUMN attendee.attendee_id IS 'not null unique. Mapped to OFFENDER_CSIP_ATTENDEES';
COMMENT ON COLUMN attendee.legacy_id IS 'unique. Prevents duplicate attendees via NOMIS -> DPS sync';
COMMENT ON COLUMN attendee.review_id IS 'not null';
COMMENT ON COLUMN attendee.name IS 'not null. Name of review attendee/contributor. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_NAME';
COMMENT ON COLUMN attendee.role IS 'not null. Role of review attendee/contributor. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_ROLE';
COMMENT ON COLUMN attendee.attended IS 'If the person attended the review. Indicates that they were a contributor if false. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDED where the default is ''N''';
COMMENT ON COLUMN attendee.contribution IS 'Description of attendee contribution. Maps to OFFENDER_CSIP_ATTENDEES.CONTRIBUTION. 4000 character limit';
COMMENT ON COLUMN attendee.version IS 'increments on each update';

-- reference_data table
COMMENT ON COLUMN reference_data.reference_data_id IS 'not null';
COMMENT ON COLUMN reference_data.domain IS 'not null composite-unique(domain+code).';
COMMENT ON COLUMN reference_data.code IS 'not null composite-unique(domain+code). The NOMIS reference data code for the CSIP_TYP, CSIP_LOC, CSIP_FUNC, CSIP_INV, CSIP_FAC, CSIP_OUT, CSIP_INTVROL, and CSIP_ROLE domain';
COMMENT ON COLUMN reference_data.created_at IS 'not null';
COMMENT ON COLUMN reference_data.created_by IS 'not null';

-- person_location table
COMMENT ON COLUMN person_location.prison_number IS 'not null';
COMMENT ON COLUMN person_location.status IS 'not null';