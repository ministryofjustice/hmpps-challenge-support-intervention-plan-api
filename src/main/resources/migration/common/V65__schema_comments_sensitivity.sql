-- Data sensitivity classification for the CSIP data dictionary.
--
-- V60 described the base tables. This migration adds a sensitivity tag to every column comment and
-- fills the gaps V60 left: the Hibernate Envers audit tables, the csip_summary view, and the
-- case_note_annotations table added later by V64.
--
-- V60 is not edited - it is applied in every environment already. COMMENT ON replaces a comment in
-- place, so re-issuing its descriptions here with a tag appended is safe and idempotent. Note that
-- V60's two statements for person_summary_audit are deliberately absent: V63 dropped that table.
--
-- Every column comment now ends with a sensitivity classification:
--
--   [Sensitivity: NONE]                - not personal data in itself (keys, timestamps, process flags)
--   [Sensitivity: PERSONAL]            - personal data about a prisoner: identifies or locates them
--   [Sensitivity: STAFF]               - personal data about a member of staff, typically the username
--                                        that performed an action
--   [Sensitivity: SPECIAL-CATEGORY]    - UK GDPR Article 9 data (health, sexuality, religion, race,
--                                        gender reassignment) or criminal offence data under Article 10
--   [Sensitivity: OFFICIAL-SENSITIVE]  - not personal data, but damaging if disclosed
--
-- STAFF is still personal data and still in scope for a staff member's own subject access request. It
-- is separated from PERSONAL so an extract about prisoners can be reasoned about without staff columns
-- inflating the count, and so staff data can be dropped or pseudonymised independently.
--
-- Four things to understand before using these classifications:
--
--   1. They describe the column's own content, not the row's. Every row here concerns a person
--      referred because they are believed to be at risk of self harm, or of harming others, so the
--      whole record is sensitive whatever an individual column is marked - that is what matters for a
--      subject access request.
--   2. This is the most Article 9 heavy schema in Manage Safety. CSIP exists to manage risk of self
--      harm and suicide. Triggers, protective factors, usual behaviour, contributory factors and the
--      screening decision are all health data about the person, not merely notes about a process.
--   3. Every free-text column should be assumed to contain more than its label asks. These fields are
--      written by staff describing an incident and a person's state of mind in their own words, and in
--      practice name third parties and describe violence, health and safeguarding concerns.
--   4. The audit tables are not a lesser copy. Envers records the full history of every mutable
--      property, so an audit row holds exactly the same special category content as the live row it
--      mirrors, and carries the same tag. A subject access request has to reach them too.
--
-- Three judgements worth challenging in review rather than taking as settled:
--
--   - person_summary.restricted_patient and supporting_prison_code are tagged SPECIAL-CATEGORY. They
--     are not health data on their face, but a restricted patient is detained under the Mental Health
--     Act, so the pair permits a direct inference about the person's mental health.
--   - csip_record.status_id is tagged SPECIAL-CATEGORY for the same reason: the status discloses that
--     someone is on, or has been on, a CSIP or ACCT pathway.
--   - case_note_annotations.annotated_text and behaviour_type are tagged SPECIAL-CATEGORY. See the
--     comment on that table - it holds verbatim case note text and a model's inference about a person.
--
-- SchemaCommentsTest fails the build if a table, view or column has no comment, or if a column comment
-- does not end in a valid tag. Keep both up to date when the schema changes.
COMMENT ON TABLE attendee IS 'Records the people who attended the review, their role and contribution';
COMMENT ON COLUMN attendee.attendee_id IS 'Public primary key [Sensitivity: NONE]';
COMMENT ON COLUMN attendee.review_id IS 'Parent review foreign key [Sensitivity: NONE]';
COMMENT ON COLUMN attendee.name IS 'Name of review attendee/contributor. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_NAME which has a 100 character limit [Sensitivity: STAFF]';
COMMENT ON COLUMN attendee.role IS 'Role of review attendee/contributor. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDEE_ROLE which has a 50 character limit [Sensitivity: STAFF]';
COMMENT ON COLUMN attendee.attended IS 'Whether the person attended the review. Indicates that they were a contributor if false. Maps to OFFENDER_CSIP_ATTENDEES.ATTENDED where the default is ''N'' [Sensitivity: NONE]';
COMMENT ON COLUMN attendee.contribution IS 'Description of attendee contribution. Maps to OFFENDER_CSIP_ATTENDEES.CONTRIBUTION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN attendee.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN attendee.legacy_id IS 'The NOMIS OFFENDER_CSIP_ATTENDEES.ATTENDEE_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';

COMMENT ON TABLE attendee_audit IS 'Attendee property changes';
COMMENT ON COLUMN attendee_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.attendee_id IS 'Audit history of attendee.attendee_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.review_id IS 'Audit history of attendee.review_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.name IS 'Audit history of attendee.name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN attendee_audit.role IS 'Audit history of attendee.role. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN attendee_audit.attended IS 'Audit history of attendee.attended. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.contribution IS 'Audit history of attendee.contribution. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN attendee_audit.name_modified IS 'Whether name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.role_modified IS 'Whether role was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.attended_modified IS 'Whether attended was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.contribution_modified IS 'Whether contribution was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN attendee_audit.legacy_id IS 'Audit history of attendee.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';

COMMENT ON TABLE audit_revision IS 'Hibernate Envers audit revision records. A revision is created for every set of changes to the CSIP entity graph and the history of every mutable property change is tracked. Full audit history starts from the 15th of November 2024';
COMMENT ON COLUMN audit_revision.id IS 'Internal primary key [Sensitivity: NONE]';
COMMENT ON COLUMN audit_revision.timestamp IS 'The date and time the change happened [Sensitivity: NONE]';
COMMENT ON COLUMN audit_revision.username IS 'The username of the user who made the change [Sensitivity: STAFF]';
COMMENT ON COLUMN audit_revision.caseload_id IS 'The active caseload of the user at the time of the change [Sensitivity: STAFF]';
COMMENT ON COLUMN audit_revision.source IS 'The system used to make the change. Either DPS or NOMIS [Sensitivity: NONE]';
COMMENT ON COLUMN audit_revision.affected_components IS 'The table or tables that had values affected by the change [Sensitivity: NONE]';

COMMENT ON TABLE case_note_annotations IS 'Machine generated annotations over a person''s prison case notes, used to suggest content for the CSIP investigation screen (CSIP Assist). Each row is one extract that a language model cited as evidence for a behaviour classification. Rows are derived data copied from the case notes service, not a record created by staff, and there is no retention or purge mechanism here.';
COMMENT ON COLUMN case_note_annotations.id IS 'Primary key. Surrogate UUID with no business meaning. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.request_id IS 'Identifier of the analysis request that produced this annotation. Groups every annotation generated by one run. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.prisoner_number IS 'NOMIS offender number (noms id) of the person whose case notes were analysed. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN case_note_annotations.case_note_id IS 'Identifier of the source case note in the case notes service. This table holds a copy of text mastered there. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.prompt_key IS 'Key of the prompt used to generate the annotation. Provenance only. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.prompt_version IS 'Version of the prompt used to generate the annotation, so annotations from a superseded prompt can be identified. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.behaviour_type IS 'What the model inferred the annotated text evidences about the person. One of RISKS_AND_TRIGGERS, USUAL_BEHAVIOUR_PRESENTATION, PROTECTIVE_FACTORS. This is a machine generated hypothesis offered to a caseworker, not a professional assessment or a recorded observation. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN case_note_annotations.confidence_level IS 'The model''s confidence in its classification of the source case note. One of LOW, MEDIUM, HIGH. Recorded per case note, so every annotation from the same case note repeats the value. [Sensitivity: NONE]';
COMMENT ON COLUMN case_note_annotations.annotated_text IS 'Verbatim extract copied from the source case note - the passage the model cites as evidence for behaviour_type. Case notes are read including those marked sensitive, so this can contain anything case note prose contains: third party names, health and mental health detail, self harm and safeguarding content. It is a duplicate of text mastered in the case notes service and is not updated or removed if the source case note changes. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN case_note_annotations.created_date IS 'When the annotation was generated. This is not the date of the source case note. [Sensitivity: NONE]';

COMMENT ON TABLE contributory_factor IS 'The contributory factors the referrer has identified as part of the referral';
COMMENT ON COLUMN contributory_factor.contributory_factor_id IS 'Public primary key [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor.referral_id IS 'Parent referral foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor.contributory_factor_type_id IS 'Foreign key to reference data using the ''CONTRIBUTORY_FACTOR_TYPE'' domain for the type of contributory factor believed to have caused the incident or behaviour. The reference_data.code value maps to OFFENDER_CSIP_FACTORS.CSIP_FACTOR [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN contributory_factor.comment IS 'Additional information about the contributory factor. Maps to OFFENDER_CSIP_FACTORS.COMMENTS which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN contributory_factor.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor.legacy_id IS 'The NOMIS OFFENDER_CSIP_FACTORS.CSIP_FACTOR_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';

COMMENT ON TABLE contributory_factor_audit IS 'Contributory factor property changes';
COMMENT ON COLUMN contributory_factor_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.contributory_factor_id IS 'Audit history of contributory_factor.contributory_factor_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.referral_id IS 'Audit history of contributory_factor.referral_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.contributory_factor_type_id IS 'Audit history of contributory_factor.contributory_factor_type_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN contributory_factor_audit.comment IS 'Audit history of contributory_factor.comment. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN contributory_factor_audit.contributory_factor_type_modified IS 'Whether contributory factor type was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.comment_modified IS 'Whether comment was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN contributory_factor_audit.legacy_id IS 'Audit history of contributory_factor.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';

COMMENT ON TABLE csip_record IS 'Root CSIP entity associating a person with a CSIP and all the child entities. Conceptually a CSIP folder. All 1:1 child entities use the record_id value for their primary key';
COMMENT ON COLUMN csip_record.record_id IS 'Public primary key for the CSIP and the shared primary key for 1:1 child entities [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record.prison_number IS 'The prison number of the person the CSIP record is for [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_record.prison_code_when_recorded IS 'The prison code where the person was resident at the time the CSIP record was created. Maps to OFFENDER_CSIP_REPORTS.AGY_LOC_ID [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_record.log_code IS 'User entered identifier for the CSIP record. Usually starts with the prison code. Maps to OFFENDER_CSIP_REPORTS.CSIP_SEQ [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record.legacy_id IS 'The NOMIS OFFENDER_CSIP_REPORTS.CSIP_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record.status_id IS 'Foreign key to reference data using the ''STATUS'' domain for the overall CSIP status. Status is calculated in NOMIS and not stored in the database [Sensitivity: SPECIAL-CATEGORY]';

COMMENT ON TABLE csip_record_audit IS 'CSIP record property changes';
COMMENT ON COLUMN csip_record_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.record_id IS 'Audit history of csip_record.record_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.prison_number IS 'Audit history of csip_record.prison_number. Holds the value as at this revision. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_record_audit.prison_code_when_recorded IS 'Audit history of csip_record.prison_code_when_recorded. Holds the value as at this revision. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_record_audit.log_code IS 'Audit history of csip_record.log_code. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.log_code_modified IS 'Whether log code was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.legacy_id IS 'Audit history of csip_record.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_record_audit.prison_number_modified IS 'Whether prison number was changed in this revision. [Sensitivity: NONE]';

COMMENT ON VIEW csip_summary IS 'Denormalised view over csip_record, person_summary, referral and plan used to search and paginate CSIPs. Read only - every column is sourced from the tables it joins.';
COMMENT ON COLUMN csip_summary.prison_number IS 'The prison number of the person the CSIP is for. Sourced from person_summary.prison_number. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_summary.first_name IS 'The first name of the person. Sourced from person_summary.first_name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_summary.last_name IS 'The last name of the person. Sourced from person_summary.last_name. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_summary.restricted_patient IS 'Whether the person is a restricted patient, i.e. detained under the Mental Health Act. Sourced from person_summary.restricted_patient. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN csip_summary.prison_code IS 'The code of the prison the person is resident at, or TRN/OUT if they are not in prison. Sourced from person_summary.prison_code. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_summary.cell_location IS 'The cell location of the person, or an alternative location reference. Sourced from person_summary.cell_location. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN csip_summary.supporting_prison_code IS 'The code of the prison supporting the person where they are a restricted patient. Sourced from person_summary.supporting_prison_code. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN csip_summary.referral_date IS 'The date the referral was submitted. Sourced from referral.referral_date. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.incident_type IS 'Description of the type of incident or behaviour that motivated the referral. Sourced from referral.incident_type_id. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN csip_summary.next_review_date IS 'The next date the plan should be reviewed. Sourced from plan.next_review_date. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.case_manager IS 'The case manager assigned to the plan. Free text, so not guaranteed to match user information. Sourced from plan.case_manager. [Sensitivity: STAFF]';
COMMENT ON COLUMN csip_summary.id IS 'The CSIP record identifier, exposed as id for the search API. Sourced from csip_record.record_id. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.log_code IS 'User entered identifier for the CSIP record. Sourced from csip_record.log_code. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.closed_date IS 'The date the plan was closed. Null unless the status is CSIP_CLOSED. Sourced from plan.closed_date. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.priority IS 'Sort weight derived from the status code, used to order search results. Carries no meaning outside the query. [Sensitivity: NONE]';
COMMENT ON COLUMN csip_summary.status_code IS 'The short code of the overall CSIP status. Sourced from csip_record.status_id. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN csip_summary.status_description IS 'The description of the overall CSIP status. Sourced from csip_record.status_id. [Sensitivity: SPECIAL-CATEGORY]';

COMMENT ON TABLE decision_and_actions IS 'Record of the decision and any actions expected as a result. Recording a decision is an optional step 4 of the CSIP process following an investigation';
COMMENT ON COLUMN decision_and_actions.decision_and_actions_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions.conclusion IS 'The conclusion and reasons for the decision. Maps to OFFENDER_CSIP_REPORTS.INV_CONCLUSION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions.outcome_id IS 'Foreign key to reference data using the ''DECISION_OUTCOME_TYPE'' domain for the selected decision. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions.signed_off_by_role_id IS 'Foreign key to reference data using the ''DECISION_SIGNER_ROLE'' domain for the role of the person who signed off the decision. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.INV_SIGNED_OFF_BY [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions.recorded_by IS 'The username of the user who recorded the decision. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_RECORDED_BY [Sensitivity: STAFF]';
COMMENT ON COLUMN decision_and_actions.recorded_by_display_name IS 'The first and last name of the user who recorded the decision. Does not update if their name changes [Sensitivity: STAFF]';
COMMENT ON COLUMN decision_and_actions.date IS 'The date the decision was made. Maps to OFFENDER_CSIP_REPORTS.INV_OUTCOME_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions.next_steps IS 'The next steps that should be taken following the decision. Maps to OFFENDER_CSIP_REPORTS.INV_NEXT_STEPS which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions.actions IS 'An enumeration of actions that have been recommended. CSIP to NOMIS mappings: ''OPEN_CSIP_ALERT'' -> OFFENDER_CSIP_REPORTS.OPEN_CSIP_ALERT, ''NON_ASSOCIATIONS_UPDATED'' -> OFFENDER_CSIP_REPORTS.INV_NON_ASSOC_BOOKED, ''OBSERVATION_BOOK'' -> OFFENDER_CSIP_REPORTS.INV_OBSERVATION_BOOK, ''UNIT_OR_CELL_MOVE'' -> OFFENDER_CSIP_REPORTS.INV_MOVE, ''CSRA_OR_RSRA_REVIEW'' -> OFFENDER_CSIP_REPORTS.INV_REVIEW, ''SERVICE_REFERRAL'' -> OFFENDER_CSIP_REPORTS.INV_SERVICE_REFERRAL, ''SIM_REFERRAL'' -> OFFENDER_CSIP_REPORTS.INV_SIM_REFERRAL. Only populated via NOMIS and retained for sync and reporting [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions.action_other IS 'Any other actions that are recommended to be considered. Maps to OFFENDER_CSIP_REPORTS.INV_OTHER which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';

COMMENT ON TABLE decision_and_actions_audit IS 'Decision and actions property changes';
COMMENT ON COLUMN decision_and_actions_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.decision_and_actions_id IS 'Audit history of decision_and_actions.decision_and_actions_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.conclusion IS 'Audit history of decision_and_actions.conclusion. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions_audit.outcome_id IS 'Audit history of decision_and_actions.outcome_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions_audit.signed_off_by_role_id IS 'Audit history of decision_and_actions.signed_off_by_role_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.recorded_by IS 'Audit history of decision_and_actions.recorded_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN decision_and_actions_audit.recorded_by_display_name IS 'Audit history of decision_and_actions.recorded_by_display_name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN decision_and_actions_audit.date IS 'Audit history of decision_and_actions.date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.next_steps IS 'Audit history of decision_and_actions.next_steps. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions_audit.actions IS 'Audit history of decision_and_actions.actions. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions_audit.action_other IS 'Audit history of decision_and_actions.action_other. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN decision_and_actions_audit.conclusion_modified IS 'Whether conclusion was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.outcome_modified IS 'Whether outcome was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.signed_off_by_modified IS 'Whether signed off by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.recorded_by_modified IS 'Whether recorded by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.recorded_by_display_name_modified IS 'Whether recorded by display name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.date_modified IS 'Whether date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.next_steps_modified IS 'Whether next steps was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.actions_modified IS 'Whether actions was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN decision_and_actions_audit.action_other_modified IS 'Whether action other was changed in this revision. [Sensitivity: NONE]';

COMMENT ON TABLE identified_need IS 'The needs identified in the plan or at subsequent reviews. Includes the plan to resolve the identified need and the progression of that plan';
COMMENT ON COLUMN identified_need.identified_need_id IS 'Public primary key [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.plan_id IS 'Parent plan foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.identified_need IS 'Details of the need identified in the CSIP plan. Maps to OFFENDER_CSIP_PLANS.IDENTIFIED_NEED which has a 1000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need.responsible_person IS 'The name of the person who is responsible for taking action on the intervention. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_PLANS.BY_WHOM which has a 100 character limit [Sensitivity: STAFF]';
COMMENT ON COLUMN identified_need.created_date IS 'The date the need was identified. Maps to OFFENDER_CSIP_PLANS.CREATE_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.target_date IS 'The target date the need should be progressed or resolved. Maps to OFFENDER_CSIP_PLANS.TARGET_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.closed_date IS 'The date the identified need was resolved and closed. Maps to OFFENDER_CSIP_PLANS.CLOSED_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.intervention IS 'The planned intervention for the identified need. Maps to OFFENDER_CSIP_PLANS.INTERVENTION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need.progression IS 'How the plan to address the identified need is progressing. Maps to OFFENDER_CSIP_PLANS.PROGRESSION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need.legacy_id IS 'The NOMIS OFFENDER_CSIP_PLANS.PLAN_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';

COMMENT ON TABLE identified_need_audit IS 'Identified needs property changes';
COMMENT ON COLUMN identified_need_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.identified_need_id IS 'Audit history of identified_need.identified_need_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.plan_id IS 'Audit history of identified_need.plan_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.identified_need IS 'Audit history of identified_need.identified_need. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need_audit.responsible_person IS 'Audit history of identified_need.responsible_person. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN identified_need_audit.created_date IS 'Audit history of identified_need.created_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.target_date IS 'Audit history of identified_need.target_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.closed_date IS 'Audit history of identified_need.closed_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.intervention IS 'Audit history of identified_need.intervention. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need_audit.progression IS 'Audit history of identified_need.progression. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN identified_need_audit.identified_need_modified IS 'Whether identified need was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.responsible_person_modified IS 'Whether responsible person was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.created_date_modified IS 'Whether created date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.target_date_modified IS 'Whether target date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.closed_date_modified IS 'Whether closed date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.intervention_modified IS 'Whether intervention was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.progression_modified IS 'Whether progression was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN identified_need_audit.legacy_id IS 'Audit history of identified_need.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';

COMMENT ON TABLE interview IS 'The interviews undertaken as part of the investigation';
COMMENT ON COLUMN interview.interview_id IS 'Public primary key [Sensitivity: NONE]';
COMMENT ON COLUMN interview.investigation_id IS 'Parent investigation foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN interview.interviewee IS 'Name of the person being interviewed. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_INTVW.CSIP_INTERVIEWEE which has a 100 character limit [Sensitivity: PERSONAL]';
COMMENT ON COLUMN interview.interview_date IS 'The date the interview took place. Maps to OFFENDER_CSIP_INTVW.INTVW_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN interview.interviewee_role_id IS 'Foreign key to reference data using the ''INTERVIEWEE_ROLE'' domain for the role the interviewee played in the incident or behaviour. Maps to OFFENDER_CSIP_INTVW.INTVW_ROLE [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN interview.interview_text IS 'Information provided in interview. Maps to OFFENDER_CSIP_INTVW.COMMENTS which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN interview.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN interview.legacy_id IS 'The NOMIS OFFENDER_CSIP_INTVW.CSIP_INTVW_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';

COMMENT ON TABLE interview_audit IS 'Interview property changes';
COMMENT ON COLUMN interview_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interview_id IS 'Audit history of interview.interview_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.investigation_id IS 'Audit history of interview.investigation_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interviewee IS 'Audit history of interview.interviewee. Holds the value as at this revision. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN interview_audit.interview_date IS 'Audit history of interview.interview_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interviewee_role_id IS 'Audit history of interview.interviewee_role_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN interview_audit.interview_text IS 'Audit history of interview.interview_text. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN interview_audit.interviewee_modified IS 'Whether interviewee was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interview_date_modified IS 'Whether interview date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interviewee_role_modified IS 'Whether interviewee role was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.interview_text_modified IS 'Whether interview text was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN interview_audit.legacy_id IS 'Audit history of interview.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';

COMMENT ON TABLE investigation IS 'The investigation that took place following a ''Progress to investigation'' screening outcome. Investigations are an optional step 3 of the CSIP process';
COMMENT ON COLUMN investigation.investigation_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN investigation.staff_involved IS 'The names of the staff involved in the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_STAFF_INVOLVED which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.evidence_secured IS 'Any evidence that was secured as part of the investigation. Maps to OFFENDER_CSIP_REPORTS.INV_EVIDENCE_SECURED which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.occurrence_reason IS 'The reasons why the incident or behaviour occurred. Maps to OFFENDER_CSIP_REPORTS.INV_OCCURRENCE_REASON which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.persons_usual_behaviour IS 'The normal behaviour of the person in prison. Maps to OFFENDER_CSIP_REPORTS.INV_USUAL_BEHAVIOUR which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.persons_trigger IS 'What triggers the person in prison has that could have motivated the incident. Maps to OFFENDER_CSIP_REPORTS.INV_PERSONS_TRIGGER which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.protective_factors IS 'Any protective factors. Maps to OFFENDER_CSIP_REPORTS.INV_PROTECTIVE_FACTORS which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN investigation.recorded_by IS 'The username of the user who recorded the investigation. New field populated from 13th of February 2025 and only via DPS [Sensitivity: STAFF]';
COMMENT ON COLUMN investigation.recorded_by_display_name IS 'The first and last name of the user who recorded the investigation. New field populated from 13th of February 2025 and only via DPS. Does not update if their name changes [Sensitivity: STAFF]';

COMMENT ON TABLE investigation_audit IS 'Investigation property changes';
COMMENT ON COLUMN investigation_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.investigation_id IS 'Audit history of investigation.investigation_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.staff_involved IS 'Audit history of investigation.staff_involved. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.evidence_secured IS 'Audit history of investigation.evidence_secured. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.occurrence_reason IS 'Audit history of investigation.occurrence_reason. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.persons_usual_behaviour IS 'Audit history of investigation.persons_usual_behaviour. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.persons_trigger IS 'Audit history of investigation.persons_trigger. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.protective_factors IS 'Audit history of investigation.protective_factors. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN investigation_audit.staff_involved_modified IS 'Whether staff involved was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.evidence_secured_modified IS 'Whether evidence secured was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.occurrence_reason_modified IS 'Whether occurrence reason was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.persons_usual_behaviour_modified IS 'Whether persons usual behaviour was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.persons_trigger_modified IS 'Whether persons trigger was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.protective_factors_modified IS 'Whether protective factors was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.recorded_by IS 'Audit history of investigation.recorded_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN investigation_audit.recorded_by_display_name IS 'Audit history of investigation.recorded_by_display_name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN investigation_audit.recorded_by_modified IS 'Whether recorded by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN investigation_audit.recorded_by_display_name_modified IS 'Whether recorded by display name was changed in this revision. [Sensitivity: NONE]';

COMMENT ON TABLE person_summary IS 'Summary data for people with CSIPs. Used to optimise searching and pagination. Domain events cause the summary data to be updated keeping it accurate';
COMMENT ON COLUMN person_summary.prison_number IS 'The prison number of the person the summary data is associated with [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.first_name IS 'The first name of the person [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.last_name IS 'The last name of the person [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.status IS 'The active/inactive in/out status of the person [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.prison_code IS 'The code of the prison the person is resident at or TRN/OUT if they are not in prison [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.cell_location IS 'The cell location of the person if they are currently in prison or an alternative location reference [Sensitivity: PERSONAL]';
COMMENT ON COLUMN person_summary.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN person_summary.restricted_patient IS 'Whether the person is a restricted patient [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN person_summary.supporting_prison_code IS 'The code of the prison a restricted patient is being supported by [Sensitivity: SPECIAL-CATEGORY]';

COMMENT ON TABLE plan IS 'The plan that was developed following a screening outcome or decision to ''Progress to CSIP''. Developing a plan is step 5 of the CSIP process';
COMMENT ON COLUMN plan.plan_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN plan.case_manager IS 'The case manager assigned to the CSIP plan. Free text so not guaranteed to match any user information. Maps to OFFENDER_CSIP_REPORTS.CASE_MANAGER which has a 100 character limit [Sensitivity: STAFF]';
COMMENT ON COLUMN plan.reason_for_plan IS 'The reasons motivating the creation of a CSIP plan. Maps to OFFENDER_CSIP_REPORTS.REASON which has a 240 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN plan.first_case_review_date IS 'The first date the CSIP plan should be reviewed. Maps to OFFENDER_CSIP_REPORTS.CASE_REV_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN plan.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN plan.next_review_date IS 'The next date the plan should be reviewed. Set as the review.review_date value from the latest review. On the plan entity for query optimisation [Sensitivity: NONE]';
COMMENT ON COLUMN plan.closed_date IS 'The date the plan was closed. Set as the review.csip_closed_date value from the first review containing an action to close the CSIP. On the plan entity for query optimisation [Sensitivity: NONE]';

COMMENT ON TABLE plan_audit IS 'Plan property changes';
COMMENT ON COLUMN plan_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.plan_id IS 'Audit history of plan.plan_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.case_manager IS 'Audit history of plan.case_manager. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN plan_audit.reason_for_plan IS 'Audit history of plan.reason_for_plan. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN plan_audit.first_case_review_date IS 'Audit history of plan.first_case_review_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.case_manager_modified IS 'Whether case manager was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.reason_for_plan_modified IS 'Whether reason for plan was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN plan_audit.first_case_review_date_modified IS 'Whether first case review date was changed in this revision. [Sensitivity: NONE]';

COMMENT ON TABLE reference_data IS 'Reference data used to populate properties that have fixed lists of valid options e.g. categories, decisions and statuses';
COMMENT ON COLUMN reference_data.reference_data_id IS 'Internal primary key. Not returned by API [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.domain IS 'The reference data domain. CSIP to NOMIS mappings: AREA_OF_WORK -> CSIP_FUNC, CONTRIBUTORY_FACTOR_TYPE -> CSIP_FAC, DECISION_OUTCOME_TYPE -> CSIP_OUT, DECISION_SIGNER_ROLE -> CSIP_ROLE, INCIDENT_INVOLVEMENT -> CSIP_INV, INCIDENT_LOCATION -> CSIP_LOC, INCIDENT_TYPE -> CSIP_TYP, INTERVIEWEE_ROLE -> CSIP_INTVROL, SCREENING_OUTCOME_TYPE -> CSIP_OUT, STATUS -> CSIP only domain [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.code IS 'The short code for the reference data item. Combination of domain and code is unique but individual codes are not guaranteed to be unique across domains. Matches the NOMIS reference data code for the associated domain [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.description IS 'The description of the reference data item [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.list_sequence IS 'Used to order reference data to guarantee the ordering for the UI [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.created_at IS 'The date and time the reference data item was created [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.created_by IS 'The username of the user who created the reference data item [Sensitivity: STAFF]';
COMMENT ON COLUMN reference_data.last_modified_at IS 'The date and time the reference data item was last modified [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.last_modified_by IS 'The username of the user who last modified the reference data item [Sensitivity: STAFF]';
COMMENT ON COLUMN reference_data.deactivated_at IS 'The date and time the reference data item was deactivated [Sensitivity: NONE]';
COMMENT ON COLUMN reference_data.deactivated_by IS 'The username of the user who deactivated the reference data item [Sensitivity: STAFF]';

COMMENT ON TABLE referral IS 'The referral that caused the CSIP record to be created. Referrals are step 1 of the CSIP process';
COMMENT ON COLUMN referral.referral_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN referral.incident_date IS 'The date the incident that motivated the CSIP referral occurred. Will be displayed on the UI as date behaviour first occurred for a proactive referral Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN referral.incident_time IS 'The time the incident that motivated the CSIP referral occurred. Will be displayed on the UI as time behaviour first occurred for a proactive referral. Maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TIME [Sensitivity: NONE]';
COMMENT ON COLUMN referral.incident_type_id IS 'Foreign key to reference data using the ''INCIDENT_TYPE'' domain for the type of incident (or behavior for a proactive referral) that motivated the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_TYPE [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.incident_location_id IS 'Foreign key to reference data using the ''INCIDENT_LOCATION'' domain for the location of the incident (or behavior for a proactive referral) that motivated the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_INCIDENT_LOCATION [Sensitivity: PERSONAL]';
COMMENT ON COLUMN referral.referred_by IS 'Either the display name of the user making the referral or free text if making the referral on behalf of someone else. Therefore not guaranteed to match user information. Maps to OFFENDER_CSIP_REPORTS.RFR_REPORTED_BY [Sensitivity: STAFF]';
COMMENT ON COLUMN referral.referer_area_of_work_id IS 'Foreign key to reference data using the ''AREA_OF_WORK'' domain for the area of work of the person making the referral. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.RFR_CSIP_FUNCTION [Sensitivity: NONE]';
COMMENT ON COLUMN referral.referral_date IS 'The date the referral was submitted. Set to today by the UI. Maps to OFFENDER_CSIP_REPORTS.RFR_DATE_REPORTED [Sensitivity: NONE]';
COMMENT ON COLUMN referral.proactive_referral IS 'Whether the referral was proactive or the result of an incident. Maps to OFFENDER_CSIP_REPORTS.RFR_PROACTIVE_RESPONSE where the default is ''N'' [Sensitivity: NONE]';
COMMENT ON COLUMN referral.staff_assaulted IS 'Whether any member(s) of staff assaulted in the incident. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_ASSAULTED where the default is ''N'' [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.assaulted_staff_name IS 'Names of any assaulted member(s) of staff. Maps to OFFENDER_CSIP_REPORTS.RFR_STAFF_NAME which has a 1000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.incident_involvement_id IS 'Foreign key to reference data using the ''INCIDENT_INVOLVEMENT'' domain for the type of involvement the person had in the incident (or behavior for a proactive referral). The reference_data.code value maps to OFFENDER_CSIP_REPORTS.CDR_INVOLVEMENT [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.description_of_concern IS 'The reasons why there is cause for concern. Maps to OFFENDER_CSIP_REPORTS.CDR_CONCERN_DESCRIPTION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.known_reasons IS 'The reasons already known about the causes of the incident or behaviour. Maps to OFFENDER_CSIP_REPORTS.INV_KNOWN_REASONS which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.other_information IS 'Any other information about the incident or behaviour. Maps to OFFENDER_CSIP_REPORTS.CDR_OTHER_INFORMATION which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral.safer_custody_team_informed IS 'Records whether the safer custody team been informed. Maps to OFFENDER_CSIP_REPORTS.CDR_SENT_DENT where the default is ''N'' [Sensitivity: NONE]';
COMMENT ON COLUMN referral.referral_complete IS 'Whether the referral complete. Will be true when DPS is used to make a referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETE_FLAG where the default is ''N'' [Sensitivity: NONE]';
COMMENT ON COLUMN referral.referral_completed_by IS 'The username of the user who completed the referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_BY [Sensitivity: STAFF]';
COMMENT ON COLUMN referral.referral_completed_by_display_name IS 'The first and last name of the user who completed the referral. Does not update if their name changes [Sensitivity: STAFF]';
COMMENT ON COLUMN referral.referral_completed_date IS 'The date the referral was completed. Will be set to today when DPS is used to make a referral. Maps to OFFENDER_CSIP_REPORTS.REFERRAL_COMPLETED_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN referral.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';

COMMENT ON TABLE referral_audit IS 'Referral property changes';
COMMENT ON COLUMN referral_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_id IS 'Audit history of referral.referral_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_date IS 'Audit history of referral.incident_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_time IS 'Audit history of referral.incident_time. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_type_id IS 'Audit history of referral.incident_type_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.incident_location_id IS 'Audit history of referral.incident_location_id. Holds the value as at this revision. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN referral_audit.referred_by IS 'Audit history of referral.referred_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN referral_audit.referer_area_of_work_id IS 'Audit history of referral.referer_area_of_work_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_date IS 'Audit history of referral.referral_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.proactive_referral IS 'Audit history of referral.proactive_referral. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.staff_assaulted IS 'Audit history of referral.staff_assaulted. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.assaulted_staff_name IS 'Audit history of referral.assaulted_staff_name. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.incident_involvement_id IS 'Audit history of referral.incident_involvement_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.description_of_concern IS 'Audit history of referral.description_of_concern. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.known_reasons IS 'Audit history of referral.known_reasons. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.other_information IS 'Audit history of referral.other_information. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN referral_audit.safer_custody_team_informed IS 'Audit history of referral.safer_custody_team_informed. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_complete IS 'Audit history of referral.referral_complete. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_completed_by IS 'Audit history of referral.referral_completed_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN referral_audit.referral_completed_by_display_name IS 'Audit history of referral.referral_completed_by_display_name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN referral_audit.referral_completed_date IS 'Audit history of referral.referral_completed_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_date_modified IS 'Whether incident date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_time_modified IS 'Whether incident time was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_type_modified IS 'Whether incident type was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_location_modified IS 'Whether incident location was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referred_by_modified IS 'Whether referred by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referer_area_of_work_modified IS 'Whether referer area of work was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_date_modified IS 'Whether referral date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.proactive_referral_modified IS 'Whether proactive referral was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.staff_assaulted_modified IS 'Whether staff assaulted was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.assaulted_staff_name_modified IS 'Whether assaulted staff name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.incident_involvement_modified IS 'Whether incident involvement was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.description_of_concern_modified IS 'Whether description of concern was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.known_reasons_modified IS 'Whether known reasons was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.other_information_modified IS 'Whether other information was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.safer_custody_team_informed_modified IS 'Whether safer custody team informed was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_complete_modified IS 'Whether referral complete was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_completed_by_modified IS 'Whether referral completed by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_completed_by_display_name_modified IS 'Whether referral completed by display name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN referral_audit.referral_completed_date_modified IS 'Whether referral completed date was changed in this revision. [Sensitivity: NONE]';

COMMENT ON TABLE review IS 'The reviews of the plan. Plans are reviewed regularly as step 6 of the CSIP process. Plans can close the CSIP completing the CSIP process';
COMMENT ON COLUMN review.review_id IS 'Public primary key [Sensitivity: NONE]';
COMMENT ON COLUMN review.plan_id IS 'Parent plan foreign key which is the shared primary key with csip_record and therefore uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN review.review_sequence IS 'The review sequence which starts at 1 and increments for each new review. Used to order the reviews. Maps to OFFENDER_CSIP_REVIEWS.REVIEW_SEQ [Sensitivity: NONE]';
COMMENT ON COLUMN review.review_date IS 'The date of the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN review.recorded_by IS 'The username of the person who recorded the review. Maps to OFFENDER_CSIP_REVIEWS.CREATE_USER [Sensitivity: STAFF]';
COMMENT ON COLUMN review.recorded_by_display_name IS 'The first and last name of the user who recorded the review. Does not update if their name changes [Sensitivity: STAFF]';
COMMENT ON COLUMN review.next_review_date IS 'The date of the next review. Maps to OFFENDER_CSIP_REVIEWS.NEXT_REVIEW_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN review.actions IS 'An enumeration of actions that should be taken following the review one of which is to close the CSIP. CSIP to NOMIS mappings: ''CLOSE_CSIP'' -> OFFENDER_CSIP_REVIEWS.CLOSE_CSIP, ''REMAIN_ON_CSIP'' -> OFFENDER_CSIP_REVIEWS.REMAIN_ON_CSIP, ''RESPONSIBLE_PEOPLE_INFORMED'' -> OFFENDER_CSIP_REVIEWS.PEOPLE_INFORMED, ''CSIP_UPDATED'' -> OFFENDER_CSIP_REVIEWS.CSIP_UPDATED, ''CASE_NOTE'' -> OFFENDER_CSIP_REVIEWS.CASE_NOTE. Only CLOSE_CSIP is populated via DPS. The rest are populated via NOMIS only and retained for sync and reporting [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN review.csip_closed_date IS 'The date the CSIP plan was closed following a review outcome decision to close it. Maps to OFFENDER_CSIP_REVIEWS.CLOSE_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN review.summary IS 'Additional information about the review. Maps to OFFENDER_CSIP_REVIEWS.SUMMARY which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN review.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';
COMMENT ON COLUMN review.legacy_id IS 'The NOMIS OFFENDER_CSIP_REVIEWS.REVIEW_ID primary key value. Stored to guarantee uniqueness via sync. Not returned by API [Sensitivity: NONE]';

COMMENT ON TABLE review_audit IS 'Review property changes';
COMMENT ON COLUMN review_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.review_id IS 'Audit history of review.review_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.plan_id IS 'Audit history of review.plan_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.review_sequence IS 'Audit history of review.review_sequence. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.review_date IS 'Audit history of review.review_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.recorded_by IS 'Audit history of review.recorded_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN review_audit.recorded_by_display_name IS 'Audit history of review.recorded_by_display_name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN review_audit.next_review_date IS 'Audit history of review.next_review_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.csip_closed_date IS 'Audit history of review.csip_closed_date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.summary IS 'Audit history of review.summary. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN review_audit.actions IS 'Audit history of review.actions. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN review_audit.review_sequence_modified IS 'Whether review sequence was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.review_date_modified IS 'Whether review date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.recorded_by_modified IS 'Whether recorded by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.recorded_by_display_name_modified IS 'Whether recorded by display name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.next_review_date_modified IS 'Whether next review date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.csip_closed_date_modified IS 'Whether csip closed date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.summary_modified IS 'Whether summary was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.actions_modified IS 'Whether actions was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN review_audit.legacy_id IS 'Audit history of review.legacy_id. Holds the value as at this revision. [Sensitivity: NONE]';

COMMENT ON TABLE safer_custody_screening_outcome IS 'The result of the referral screening process undertaken by the Safer Custody team. Screening is step 2 of the CSIP process which can end if the decision is not to proceed further';
COMMENT ON COLUMN safer_custody_screening_outcome.safer_custody_screening_outcome_id IS 'Shared primary key with csip_record. Uses the same value as csip_record.record_id [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome.outcome_id IS 'Foreign key to reference data using the ''SCREENING_OUTCOME_TYPE'' domain for the selected screening outcome. The reference_data.code value maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by IS 'The username of the user who recorded the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_RECORDED_BY [Sensitivity: STAFF]';
COMMENT ON COLUMN safer_custody_screening_outcome.recorded_by_display_name IS 'The first and last name of the user who recorded the safer custody screening outcome. Does not update if their name changes [Sensitivity: STAFF]';
COMMENT ON COLUMN safer_custody_screening_outcome.date IS 'The date of the safer custody screening outcome. Maps to OFFENDER_CSIP_REPORTS.CDR_OUTCOME_DATE [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome.reason_for_decision IS 'The reasons for the safer custody screening outcome decision. Maps to OFFENDER_CSIP_REPORTS.CDR_DECISION_REASON which has a 4000 character limit [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN safer_custody_screening_outcome.version IS 'Supports optimistic locking preventing unnecessary select queries for new entities [Sensitivity: NONE]';

COMMENT ON TABLE safer_custody_screening_outcome_audit IS 'Screening property changes';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.rev_id IS 'Foreign key to audit_revision. Identifies the revision this row belongs to. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.rev_type IS 'Type of change; 0 -> entity was created, 1 -> entity was updated, 2 -> entity was deleted [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.safer_custody_screening_outcome_id IS 'Audit history of safer_custody_screening_outcome.safer_custody_screening_outcome_id. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.outcome_id IS 'Audit history of safer_custody_screening_outcome.outcome_id. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.recorded_by IS 'Audit history of safer_custody_screening_outcome.recorded_by. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.recorded_by_display_name IS 'Audit history of safer_custody_screening_outcome.recorded_by_display_name. Holds the value as at this revision. [Sensitivity: STAFF]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.date IS 'Audit history of safer_custody_screening_outcome.date. Holds the value as at this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.reason_for_decision IS 'Audit history of safer_custody_screening_outcome.reason_for_decision. Holds the value as at this revision. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.outcome_modified IS 'Whether outcome was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.recorded_by_modified IS 'Whether recorded by was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.recorded_by_display_name_modified IS 'Whether recorded by display name was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.date_modified IS 'Whether date was changed in this revision. [Sensitivity: NONE]';
COMMENT ON COLUMN safer_custody_screening_outcome_audit.reason_for_decision_modified IS 'Whether reason for decision was changed in this revision. [Sensitivity: NONE]';
