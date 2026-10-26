-- Example values and subject access request (SAR) classification for every column (IR-2067).
--
-- These feed the SAR Data Requirements extract (scripts/generate-sar-data-requirements.sh), the document the
-- Offender SAR team review at the data review checkpoint of the SAR change control process. CSIP has never been
-- registered with the SAR tool, so it goes through that process as a New Product, and every element needs an
-- example value and a statement of whether it reaches a prisoner's report. Comments only - no schema or data
-- change.
--
-- Each column comment gains two tags, between the description and the existing sensitivity tag:
--
--   '... description text. [Example: VPA] [SAR: Y] [Sensitivity: SPECIAL-CATEGORY]'
--
-- [SAR: Y] means the value reaches the report under the response proposed for the data review. The response
-- does not exist yet, so these are a proposal for the Offender SAR team to confirm or change; their decision
-- goes in a later migration rather than an edit to this one. In outline:
--
--   * Y: what was recorded at each stage of the CSIP - referral, screening, investigation and interviews,
--     decision, plan and reviews. Codes are shown as their descriptions. Staff who recorded a stage are shown
--     by surname only, the rule the Offender SAR team set for incident reporting, so the *_display_name columns
--     are Y and the username columns beside them N.
--   * Y, for the Offender SAR team to redact: other people named in a CSIP - interviewees, attendees and the
--     free-text name columns (referred_by, case_manager, responsible_person, staff_involved,
--     assaulted_staff_name). These are typed by staff and cannot be reliably reduced to a surname.
--   * N: internal keys, version and legacy_id, the requester's own prison number (the SAR tool prints it on
--     every page), the copies in person_summary and csip_summary that exist to speed up searches,
--     plan.next_review_date and plan.closed_date (copies of review values), and reference_data, which is
--     used to decode codes but is not about any person.
--   * N, put to the Offender SAR team as questions: the Envers *_audit tables and audit_revision, which hold
--     every earlier version of a CSIP since 15 November 2024 and are not shown to staff - the proposal is the
--     current version only; and the CSIP Assist tables (case_notes_analysed, case_note_annotations), which hold
--     copies of case notes and model output, and are switched off in production.
--
-- Example values for code columns are real codes. Every other example is invented, and the free-text examples
-- are deliberately bland: they illustrate the kind of content, not real CSIPs.

DO $$
DECLARE
  rec      record;
  existing text;
BEGIN
  FOR rec IN
    SELECT * FROM (VALUES
      -- CSIP record
      ('csip_record', 'record_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('csip_record', 'prison_number', 'A1234BC', 'N'),
      ('csip_record', 'prison_code_when_recorded', 'MDI', 'Y'),
      ('csip_record', 'log_code', 'MDI001', 'Y'),
      ('csip_record', 'version', '0', 'N'),
      ('csip_record', 'legacy_id', '123456', 'N'),
      ('csip_record', 'status_id', 'CSIP_OPEN', 'Y'),

      -- Referral
      ('referral', 'referral_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('referral', 'incident_date', '2026-03-14', 'Y'),
      ('referral', 'incident_time', '09:30:00', 'Y'),
      ('referral', 'incident_type_id', 'VPA', 'Y'),
      ('referral', 'incident_location_id', 'WIL', 'Y'),
      ('referral', 'referred_by', 'Jane Doe', 'Y'),
      ('referral', 'referer_area_of_work_id', 'RSD', 'Y'),
      ('referral', 'referral_date', '2026-03-15', 'Y'),
      ('referral', 'proactive_referral', 'false', 'Y'),
      ('referral', 'staff_assaulted', 'true', 'Y'),
      ('referral', 'assaulted_staff_name', 'Officer Brown', 'Y'),
      ('referral', 'incident_involvement_id', 'PER', 'Y'),
      ('referral', 'description_of_concern', 'Two fights on the wing in a week, and he has said he is in debt to others on the landing.', 'Y'),
      ('referral', 'known_reasons', 'Reports he owes money for vapes.', 'Y'),
      ('referral', 'other_information', 'Recently moved from another prison.', 'Y'),
      ('referral', 'safer_custody_team_informed', 'YES', 'Y'),
      ('referral', 'referral_complete', 'true', 'N'),
      ('referral', 'referral_completed_by', 'AUSER_GEN', 'N'),
      ('referral', 'referral_completed_by_display_name', 'Jane Doe', 'Y'),
      ('referral', 'referral_completed_date', '2026-03-15', 'Y'),
      ('referral', 'version', '0', 'N'),

      ('contributory_factor', 'contributory_factor_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('contributory_factor', 'referral_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('contributory_factor', 'contributory_factor_type_id', 'DEB', 'Y'),
      ('contributory_factor', 'comment', 'Owes money for vapes.', 'Y'),
      ('contributory_factor', 'version', '0', 'N'),
      ('contributory_factor', 'legacy_id', '123456', 'N'),

      -- Screening
      ('safer_custody_screening_outcome', 'safer_custody_screening_outcome_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('safer_custody_screening_outcome', 'outcome_id', 'OPE', 'Y'),
      ('safer_custody_screening_outcome', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('safer_custody_screening_outcome', 'recorded_by_display_name', 'John Smith', 'Y'),
      ('safer_custody_screening_outcome', 'date', '2026-03-16', 'Y'),
      ('safer_custody_screening_outcome', 'reason_for_decision', 'Pattern of violence linked to debt needs looking into.', 'Y'),
      ('safer_custody_screening_outcome', 'version', '0', 'N'),

      -- Investigation
      ('investigation', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('investigation', 'staff_involved', 'Officer Brown, Officer Green', 'Y'),
      ('investigation', 'evidence_secured', 'Wing CCTV for 14 March.', 'Y'),
      ('investigation', 'occurrence_reason', 'Pressure to repay a debt.', 'Y'),
      ('investigation', 'persons_usual_behaviour', 'Usually calm and polite, works in the kitchen.', 'Y'),
      ('investigation', 'persons_trigger', 'Being approached about money.', 'Y'),
      ('investigation', 'protective_factors', 'Regular family visits and his job.', 'Y'),
      ('investigation', 'version', '0', 'N'),
      ('investigation', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('investigation', 'recorded_by_display_name', 'John Smith', 'Y'),

      ('interview', 'interview_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('interview', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('interview', 'interviewee', 'Joe Bloggs', 'Y'),
      ('interview', 'interview_date', '2026-03-18', 'Y'),
      ('interview', 'interviewee_role_id', 'WITNESS', 'Y'),
      ('interview', 'interview_text', 'Saw the fight start near the servery.', 'Y'),
      ('interview', 'version', '0', 'N'),
      ('interview', 'legacy_id', '123456', 'N'),

      -- Decision
      ('decision_and_actions', 'decision_and_actions_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('decision_and_actions', 'conclusion', 'Debt is the main cause; a plan is needed.', 'Y'),
      ('decision_and_actions', 'outcome_id', 'CUR', 'Y'),
      ('decision_and_actions', 'signed_off_by_role_id', 'CUSTMAN', 'Y'),
      ('decision_and_actions', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('decision_and_actions', 'recorded_by_display_name', 'John Smith', 'Y'),
      ('decision_and_actions', 'date', '2026-03-20', 'Y'),
      ('decision_and_actions', 'next_steps', 'Open a CSIP and refer to the debt advice service.', 'Y'),
      ('decision_and_actions', 'actions', 'OPEN_CSIP_ALERT, SERVICE_REFERRAL', 'Y'),
      ('decision_and_actions', 'action_other', 'Speak to the wing manager about a landing move.', 'Y'),
      ('decision_and_actions', 'version', '0', 'N'),

      -- Plan
      ('plan', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('plan', 'case_manager', 'Jane Doe', 'Y'),
      ('plan', 'reason_for_plan', 'Violence linked to debt.', 'Y'),
      ('plan', 'first_case_review_date', '2026-04-20', 'Y'),
      ('plan', 'version', '0', 'N'),
      ('plan', 'next_review_date', '2026-05-20', 'N'),
      ('plan', 'closed_date', '2026-06-30', 'N'),

      ('identified_need', 'identified_need_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('identified_need', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('identified_need', 'identified_need', 'Support to manage debt.', 'Y'),
      ('identified_need', 'responsible_person', 'Jane Doe', 'Y'),
      ('identified_need', 'created_date', '2026-03-21', 'Y'),
      ('identified_need', 'target_date', '2026-04-21', 'Y'),
      ('identified_need', 'closed_date', '2026-05-01', 'Y'),
      ('identified_need', 'intervention', 'Weekly meeting with the debt advice worker.', 'Y'),
      ('identified_need', 'progression', 'Attended three of four sessions.', 'Y'),
      ('identified_need', 'version', '0', 'N'),
      ('identified_need', 'legacy_id', '123456', 'N'),

      -- Reviews
      ('review', 'review_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('review', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('review', 'review_sequence', '1', 'N'),
      ('review', 'review_date', '2026-04-20', 'Y'),
      ('review', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('review', 'recorded_by_display_name', 'John Smith', 'Y'),
      ('review', 'next_review_date', '2026-05-20', 'Y'),
      ('review', 'actions', 'REMAIN_ON_CSIP', 'Y'),
      ('review', 'csip_closed_date', '2026-06-30', 'Y'),
      ('review', 'summary', 'Good progress, no further incidents.', 'Y'),
      ('review', 'version', '0', 'N'),
      ('review', 'legacy_id', '123456', 'N'),

      ('attendee', 'attendee_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('attendee', 'review_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('attendee', 'name', 'Jane Doe', 'Y'),
      ('attendee', 'role', 'Wing manager', 'Y'),
      ('attendee', 'attended', 'true', 'Y'),
      ('attendee', 'contribution', 'Says he is engaging well with the plan.', 'Y'),
      ('attendee', 'version', '0', 'N'),
      ('attendee', 'legacy_id', '123456', 'N'),

      -- CSIP Assist (off in production)
      ('case_notes_analysed', 'id', '0192a3b4-1111-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_notes_analysed', 'request_id', '0192a3b4-2222-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_notes_analysed', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_notes_analysed', 'prisoner_number', 'A1234BC', 'N'),
      ('case_notes_analysed', 'case_note_id', '0192a3b4-3333-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_notes_analysed', 'prompt_key', 'case-note-analysis', 'N'),
      ('case_notes_analysed', 'prompt_version', '2', 'N'),
      ('case_notes_analysed', 'usual_behaviour_relevancy', '3', 'N'),
      ('case_notes_analysed', 'risks_and_triggers_relevancy', '8', 'N'),
      ('case_notes_analysed', 'protective_factors_relevancy', '1', 'N'),

      ('case_note_annotations', 'id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_note_annotations', 'case_notes_analysed_id', '0192a3b4-1111-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_note_annotations', 'request_id', '0192a3b4-2222-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_note_annotations', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_note_annotations', 'case_note_id', '0192a3b4-3333-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('case_note_annotations', 'behaviour_type', 'RISKS_AND_TRIGGERS', 'N'),
      ('case_note_annotations', 'annotated_text', 'became agitated when asked about money he owed', 'N'),
      ('case_note_annotations', 'created_date', '2026-03-18T10:15:00', 'N'),

      -- Search copies
      ('person_summary', 'prison_number', 'A1234BC', 'N'),
      ('person_summary', 'first_name', 'John', 'N'),
      ('person_summary', 'last_name', 'Smith', 'N'),
      ('person_summary', 'status', 'ACTIVE IN', 'N'),
      ('person_summary', 'prison_code', 'MDI', 'N'),
      ('person_summary', 'cell_location', '1-2-003', 'N'),
      ('person_summary', 'version', '0', 'N'),
      ('person_summary', 'restricted_patient', 'false', 'N'),
      ('person_summary', 'supporting_prison_code', 'MDI', 'N'),

      ('csip_summary', 'prison_number', 'A1234BC', 'N'),
      ('csip_summary', 'first_name', 'John', 'N'),
      ('csip_summary', 'last_name', 'Smith', 'N'),
      ('csip_summary', 'restricted_patient', 'false', 'N'),
      ('csip_summary', 'prison_code', 'MDI', 'N'),
      ('csip_summary', 'cell_location', '1-2-003', 'N'),
      ('csip_summary', 'supporting_prison_code', 'MDI', 'N'),
      ('csip_summary', 'referral_date', '2026-03-15', 'N'),
      ('csip_summary', 'incident_type', 'Violence - prisoner assault', 'N'),
      ('csip_summary', 'next_review_date', '2026-05-20', 'N'),
      ('csip_summary', 'case_manager', 'Jane Doe', 'N'),
      ('csip_summary', 'id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('csip_summary', 'log_code', 'MDI001', 'N'),
      ('csip_summary', 'closed_date', '2026-06-30', 'N'),
      ('csip_summary', 'priority', '3', 'N'),
      ('csip_summary', 'status_code', 'CSIP_OPEN', 'N'),
      ('csip_summary', 'status_description', 'CSIP open', 'N'),

      -- Code lists
      ('reference_data', 'reference_data_id', '42', 'N'),
      ('reference_data', 'domain', 'INCIDENT_TYPE', 'N'),
      ('reference_data', 'code', 'VPA', 'N'),
      ('reference_data', 'description', 'Violence - prisoner assault', 'N'),
      ('reference_data', 'list_sequence', '17', 'N'),
      ('reference_data', 'created_at', '2024-06-01T10:00:00', 'N'),
      ('reference_data', 'created_by', 'SYS', 'N'),
      ('reference_data', 'last_modified_at', '2024-06-01T10:00:00', 'N'),
      ('reference_data', 'last_modified_by', 'SYS', 'N'),
      ('reference_data', 'deactivated_at', '2025-01-01T00:00:00', 'N'),
      ('reference_data', 'deactivated_by', 'SYS', 'N'),

      -- Change history
      ('audit_revision', 'id', '1234', 'N'),
      ('audit_revision', 'timestamp', '2026-03-15T11:05:12', 'N'),
      ('audit_revision', 'username', 'AUSER_GEN', 'N'),
      ('audit_revision', 'caseload_id', 'MDI', 'N'),
      ('audit_revision', 'source', 'DPS', 'N'),
      ('audit_revision', 'affected_components', 'REFERRAL, CONTRIBUTORY_FACTOR', 'N'),

      ('csip_record_audit', 'rev_id', '1234', 'N'),
      ('csip_record_audit', 'rev_type', '1', 'N'),
      ('csip_record_audit', 'record_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('csip_record_audit', 'prison_number', 'A1234BC', 'N'),
      ('csip_record_audit', 'prison_code_when_recorded', 'MDI', 'N'),
      ('csip_record_audit', 'log_code', 'MDI001', 'N'),
      ('csip_record_audit', 'log_code_modified', 'true', 'N'),
      ('csip_record_audit', 'legacy_id', '123456', 'N'),
      ('csip_record_audit', 'prison_number_modified', 'true', 'N'),

      ('referral_audit', 'rev_id', '1234', 'N'),
      ('referral_audit', 'rev_type', '1', 'N'),
      ('referral_audit', 'referral_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('referral_audit', 'incident_date', '2026-03-14', 'N'),
      ('referral_audit', 'incident_time', '09:30:00', 'N'),
      ('referral_audit', 'incident_type_id', 'VPA', 'N'),
      ('referral_audit', 'incident_location_id', 'WIL', 'N'),
      ('referral_audit', 'referred_by', 'Jane Doe', 'N'),
      ('referral_audit', 'referer_area_of_work_id', 'RSD', 'N'),
      ('referral_audit', 'referral_date', '2026-03-15', 'N'),
      ('referral_audit', 'proactive_referral', 'false', 'N'),
      ('referral_audit', 'staff_assaulted', 'true', 'N'),
      ('referral_audit', 'assaulted_staff_name', 'Officer Brown', 'N'),
      ('referral_audit', 'incident_involvement_id', 'PER', 'N'),
      ('referral_audit', 'description_of_concern', 'Two fights on the wing in a week, and he has said he is in debt to others on the landing.', 'N'),
      ('referral_audit', 'known_reasons', 'Reports he owes money for vapes.', 'N'),
      ('referral_audit', 'other_information', 'Recently moved from another prison.', 'N'),
      ('referral_audit', 'safer_custody_team_informed', 'YES', 'N'),
      ('referral_audit', 'referral_complete', 'true', 'N'),
      ('referral_audit', 'referral_completed_by', 'AUSER_GEN', 'N'),
      ('referral_audit', 'referral_completed_by_display_name', 'Jane Doe', 'N'),
      ('referral_audit', 'referral_completed_date', '2026-03-15', 'N'),
      ('referral_audit', 'incident_date_modified', 'true', 'N'),
      ('referral_audit', 'incident_time_modified', 'true', 'N'),
      ('referral_audit', 'incident_type_modified', 'true', 'N'),
      ('referral_audit', 'incident_location_modified', 'true', 'N'),
      ('referral_audit', 'referred_by_modified', 'true', 'N'),
      ('referral_audit', 'referer_area_of_work_modified', 'true', 'N'),
      ('referral_audit', 'referral_date_modified', 'true', 'N'),
      ('referral_audit', 'proactive_referral_modified', 'true', 'N'),
      ('referral_audit', 'staff_assaulted_modified', 'true', 'N'),
      ('referral_audit', 'assaulted_staff_name_modified', 'true', 'N'),
      ('referral_audit', 'incident_involvement_modified', 'true', 'N'),
      ('referral_audit', 'description_of_concern_modified', 'true', 'N'),
      ('referral_audit', 'known_reasons_modified', 'true', 'N'),
      ('referral_audit', 'other_information_modified', 'true', 'N'),
      ('referral_audit', 'safer_custody_team_informed_modified', 'true', 'N'),
      ('referral_audit', 'referral_complete_modified', 'true', 'N'),
      ('referral_audit', 'referral_completed_by_modified', 'true', 'N'),
      ('referral_audit', 'referral_completed_by_display_name_modified', 'true', 'N'),
      ('referral_audit', 'referral_completed_date_modified', 'true', 'N'),

      ('contributory_factor_audit', 'rev_id', '1234', 'N'),
      ('contributory_factor_audit', 'rev_type', '1', 'N'),
      ('contributory_factor_audit', 'contributory_factor_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('contributory_factor_audit', 'referral_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('contributory_factor_audit', 'contributory_factor_type_id', 'DEB', 'N'),
      ('contributory_factor_audit', 'comment', 'Owes money for vapes.', 'N'),
      ('contributory_factor_audit', 'contributory_factor_type_modified', 'true', 'N'),
      ('contributory_factor_audit', 'comment_modified', 'true', 'N'),
      ('contributory_factor_audit', 'legacy_id', '123456', 'N'),

      ('safer_custody_screening_outcome_audit', 'rev_id', '1234', 'N'),
      ('safer_custody_screening_outcome_audit', 'rev_type', '1', 'N'),
      ('safer_custody_screening_outcome_audit', 'safer_custody_screening_outcome_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('safer_custody_screening_outcome_audit', 'outcome_id', 'OPE', 'N'),
      ('safer_custody_screening_outcome_audit', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('safer_custody_screening_outcome_audit', 'recorded_by_display_name', 'John Smith', 'N'),
      ('safer_custody_screening_outcome_audit', 'date', '2026-03-16', 'N'),
      ('safer_custody_screening_outcome_audit', 'reason_for_decision', 'Pattern of violence linked to debt needs looking into.', 'N'),
      ('safer_custody_screening_outcome_audit', 'outcome_modified', 'true', 'N'),
      ('safer_custody_screening_outcome_audit', 'recorded_by_modified', 'true', 'N'),
      ('safer_custody_screening_outcome_audit', 'recorded_by_display_name_modified', 'true', 'N'),
      ('safer_custody_screening_outcome_audit', 'date_modified', 'true', 'N'),
      ('safer_custody_screening_outcome_audit', 'reason_for_decision_modified', 'true', 'N'),

      ('investigation_audit', 'rev_id', '1234', 'N'),
      ('investigation_audit', 'rev_type', '1', 'N'),
      ('investigation_audit', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('investigation_audit', 'staff_involved', 'Officer Brown, Officer Green', 'N'),
      ('investigation_audit', 'evidence_secured', 'Wing CCTV for 14 March.', 'N'),
      ('investigation_audit', 'occurrence_reason', 'Pressure to repay a debt.', 'N'),
      ('investigation_audit', 'persons_usual_behaviour', 'Usually calm and polite, works in the kitchen.', 'N'),
      ('investigation_audit', 'persons_trigger', 'Being approached about money.', 'N'),
      ('investigation_audit', 'protective_factors', 'Regular family visits and his job.', 'N'),
      ('investigation_audit', 'staff_involved_modified', 'true', 'N'),
      ('investigation_audit', 'evidence_secured_modified', 'true', 'N'),
      ('investigation_audit', 'occurrence_reason_modified', 'true', 'N'),
      ('investigation_audit', 'persons_usual_behaviour_modified', 'true', 'N'),
      ('investigation_audit', 'persons_trigger_modified', 'true', 'N'),
      ('investigation_audit', 'protective_factors_modified', 'true', 'N'),
      ('investigation_audit', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('investigation_audit', 'recorded_by_display_name', 'John Smith', 'N'),
      ('investigation_audit', 'recorded_by_modified', 'true', 'N'),
      ('investigation_audit', 'recorded_by_display_name_modified', 'true', 'N'),

      ('interview_audit', 'rev_id', '1234', 'N'),
      ('interview_audit', 'rev_type', '1', 'N'),
      ('interview_audit', 'interview_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('interview_audit', 'investigation_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('interview_audit', 'interviewee', 'Joe Bloggs', 'N'),
      ('interview_audit', 'interview_date', '2026-03-18', 'N'),
      ('interview_audit', 'interviewee_role_id', 'WITNESS', 'N'),
      ('interview_audit', 'interview_text', 'Saw the fight start near the servery.', 'N'),
      ('interview_audit', 'interviewee_modified', 'true', 'N'),
      ('interview_audit', 'interview_date_modified', 'true', 'N'),
      ('interview_audit', 'interviewee_role_modified', 'true', 'N'),
      ('interview_audit', 'interview_text_modified', 'true', 'N'),
      ('interview_audit', 'legacy_id', '123456', 'N'),

      ('decision_and_actions_audit', 'rev_id', '1234', 'N'),
      ('decision_and_actions_audit', 'rev_type', '1', 'N'),
      ('decision_and_actions_audit', 'decision_and_actions_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('decision_and_actions_audit', 'conclusion', 'Debt is the main cause; a plan is needed.', 'N'),
      ('decision_and_actions_audit', 'outcome_id', 'CUR', 'N'),
      ('decision_and_actions_audit', 'signed_off_by_role_id', 'CUSTMAN', 'N'),
      ('decision_and_actions_audit', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('decision_and_actions_audit', 'recorded_by_display_name', 'John Smith', 'N'),
      ('decision_and_actions_audit', 'date', '2026-03-20', 'N'),
      ('decision_and_actions_audit', 'next_steps', 'Open a CSIP and refer to the debt advice service.', 'N'),
      ('decision_and_actions_audit', 'actions', 'OPEN_CSIP_ALERT, SERVICE_REFERRAL', 'N'),
      ('decision_and_actions_audit', 'action_other', 'Speak to the wing manager about a landing move.', 'N'),
      ('decision_and_actions_audit', 'conclusion_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'outcome_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'signed_off_by_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'recorded_by_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'recorded_by_display_name_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'date_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'next_steps_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'actions_modified', 'true', 'N'),
      ('decision_and_actions_audit', 'action_other_modified', 'true', 'N'),

      ('plan_audit', 'rev_id', '1234', 'N'),
      ('plan_audit', 'rev_type', '1', 'N'),
      ('plan_audit', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('plan_audit', 'case_manager', 'Jane Doe', 'N'),
      ('plan_audit', 'reason_for_plan', 'Violence linked to debt.', 'N'),
      ('plan_audit', 'first_case_review_date', '2026-04-20', 'N'),
      ('plan_audit', 'case_manager_modified', 'true', 'N'),
      ('plan_audit', 'reason_for_plan_modified', 'true', 'N'),
      ('plan_audit', 'first_case_review_date_modified', 'true', 'N'),

      ('identified_need_audit', 'rev_id', '1234', 'N'),
      ('identified_need_audit', 'rev_type', '1', 'N'),
      ('identified_need_audit', 'identified_need_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('identified_need_audit', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('identified_need_audit', 'identified_need', 'Support to manage debt.', 'N'),
      ('identified_need_audit', 'responsible_person', 'Jane Doe', 'N'),
      ('identified_need_audit', 'created_date', '2026-03-21', 'N'),
      ('identified_need_audit', 'target_date', '2026-04-21', 'N'),
      ('identified_need_audit', 'closed_date', '2026-05-01', 'N'),
      ('identified_need_audit', 'intervention', 'Weekly meeting with the debt advice worker.', 'N'),
      ('identified_need_audit', 'progression', 'Attended three of four sessions.', 'N'),
      ('identified_need_audit', 'identified_need_modified', 'true', 'N'),
      ('identified_need_audit', 'responsible_person_modified', 'true', 'N'),
      ('identified_need_audit', 'created_date_modified', 'true', 'N'),
      ('identified_need_audit', 'target_date_modified', 'true', 'N'),
      ('identified_need_audit', 'closed_date_modified', 'true', 'N'),
      ('identified_need_audit', 'intervention_modified', 'true', 'N'),
      ('identified_need_audit', 'progression_modified', 'true', 'N'),
      ('identified_need_audit', 'legacy_id', '123456', 'N'),

      ('review_audit', 'rev_id', '1234', 'N'),
      ('review_audit', 'rev_type', '1', 'N'),
      ('review_audit', 'review_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('review_audit', 'plan_id', '0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('review_audit', 'review_sequence', '1', 'N'),
      ('review_audit', 'review_date', '2026-04-20', 'N'),
      ('review_audit', 'recorded_by', 'BSTAFF_GEN', 'N'),
      ('review_audit', 'recorded_by_display_name', 'John Smith', 'N'),
      ('review_audit', 'next_review_date', '2026-05-20', 'N'),
      ('review_audit', 'csip_closed_date', '2026-06-30', 'N'),
      ('review_audit', 'summary', 'Good progress, no further incidents.', 'N'),
      ('review_audit', 'actions', 'REMAIN_ON_CSIP', 'N'),
      ('review_audit', 'review_sequence_modified', 'true', 'N'),
      ('review_audit', 'review_date_modified', 'true', 'N'),
      ('review_audit', 'recorded_by_modified', 'true', 'N'),
      ('review_audit', 'recorded_by_display_name_modified', 'true', 'N'),
      ('review_audit', 'next_review_date_modified', 'true', 'N'),
      ('review_audit', 'csip_closed_date_modified', 'true', 'N'),
      ('review_audit', 'summary_modified', 'true', 'N'),
      ('review_audit', 'actions_modified', 'true', 'N'),
      ('review_audit', 'legacy_id', '123456', 'N'),

      ('attendee_audit', 'rev_id', '1234', 'N'),
      ('attendee_audit', 'rev_type', '1', 'N'),
      ('attendee_audit', 'attendee_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('attendee_audit', 'review_id', '0192a3b4-0000-7e8f-9a0b-1c2d3e4f5a6b', 'N'),
      ('attendee_audit', 'name', 'Jane Doe', 'N'),
      ('attendee_audit', 'role', 'Wing manager', 'N'),
      ('attendee_audit', 'attended', 'true', 'N'),
      ('attendee_audit', 'contribution', 'Says he is engaging well with the plan.', 'N'),
      ('attendee_audit', 'name_modified', 'true', 'N'),
      ('attendee_audit', 'role_modified', 'true', 'N'),
      ('attendee_audit', 'attended_modified', 'true', 'N'),
      ('attendee_audit', 'contribution_modified', 'true', 'N'),
      ('attendee_audit', 'legacy_id', '123456', 'N')
    ) AS t(table_name, column_name, example, sar_impact)
  LOOP
    SELECT col_description(a.attrelid, a.attnum)
      INTO existing
      FROM pg_attribute a
     WHERE a.attrelid = format('%I', rec.table_name)::regclass
       AND a.attname = rec.column_name
       AND a.attnum > 0
       AND NOT a.attisdropped;

    IF existing IS NULL THEN
      RAISE EXCEPTION 'No comment on %.% - describe the column before classifying it',
        rec.table_name, rec.column_name;
    END IF;

    IF position(' [Sensitivity:' IN existing) = 0 THEN
      RAISE EXCEPTION 'Comment on %.% has no sensitivity tag to place the new tags before',
        rec.table_name, rec.column_name;
    END IF;

    IF position('[Example:' IN existing) > 0 OR position('[SAR:' IN existing) > 0 THEN
      RAISE EXCEPTION 'Comment on %.% is already tagged', rec.table_name, rec.column_name;
    END IF;

    IF position(']' IN rec.example) > 0 THEN
      RAISE EXCEPTION 'Example for %.% contains "]", which would end the tag early',
        rec.table_name, rec.column_name;
    END IF;

    EXECUTE format(
      'COMMENT ON COLUMN %I.%I IS %L',
      rec.table_name,
      rec.column_name,
      replace(
        existing,
        ' [Sensitivity:',
        ' [Example: ' || rec.example || '] [SAR: ' || rec.sar_impact || '] [Sensitivity:'
      )
    );
  END LOOP;
END $$;
