alter table csip_record
    add column legacy_id bigint unique nulls distinct;

alter table csip_record_audit
    add column legacy_id bigint,
    add column prison_number_modified boolean default false;

alter table contributory_factor
    add column legacy_id bigint unique nulls distinct;

alter table contributory_factor_audit
    add column legacy_id bigint;

alter table interview
    add column legacy_id bigint unique nulls distinct;

alter table interview_audit
    add column legacy_id bigint;

alter table identified_need
    add column legacy_id bigint unique nulls distinct;

alter table identified_need_audit
    add column legacy_id bigint;

alter table review
    add column legacy_id bigint unique nulls distinct;

alter table review_audit
    add column legacy_id bigint;

alter table attendee
    add column legacy_id bigint unique nulls distinct;

alter table attendee_audit
    add column legacy_id bigint;