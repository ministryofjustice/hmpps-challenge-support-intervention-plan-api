alter table referral
    alter column incident_type_id type bigint,
    alter column incident_location_id type bigint,
    alter column referer_area_of_work_id type bigint,
    alter column incident_involvement_id type bigint
;

alter table safer_custody_screening_outcome
    alter column outcome_id type bigint
;

alter table decision_and_actions
    alter column outcome_id type bigint,
    alter column signed_off_by_role_id type bigint
;

alter table contributory_factor
    alter column referral_id type bigint,
    alter column contributory_factor_type_id type bigint
;

alter table interview
    alter column investigation_id type bigint,
    alter column interviewee_role_id type bigint
;

alter table identified_need
    alter column plan_id type bigint
;

alter table review
    alter column plan_id type bigint
;

alter table attendee
    alter column review_id type bigint
;

