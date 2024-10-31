alter table attendee
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table attendee_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table audit_revision
    drop column user_display_name;

alter table contributory_factor
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table contributory_factor_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table csip_record
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table csip_record_audit
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table decision_and_actions
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table decision_and_actions_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table identified_need
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table identified_need_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table interview
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table interview_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table investigation
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table investigation_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table plan
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table plan_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table referral
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table referral_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table review
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table review_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table safer_custody_screening_outcome
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;

alter table safer_custody_screening_outcome_audit
    drop column created_at,
    drop column created_by,
    drop column created_by_display_name,
    drop column last_modified_at,
    drop column last_modified_by,
    drop column last_modified_by_display_name;