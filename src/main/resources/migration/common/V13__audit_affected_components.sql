drop table audit_event;

create table audit_event
(
    id                        bigserial primary key not null,
    csip_record_id            bigserial             not null,
    action                    varchar(40)           not null,
    description               text                  not null,
    actioned_at               timestamp             not null,
    actioned_by               varchar(32)           not null,
    actioned_by_captured_name varchar(255)          not null,
    source                    varchar(12)           not null,
    active_case_load_id       varchar(6),
    affected_components        varchar[]             not null,
    foreign key (csip_record_id) references csip_record (record_id)
);

alter table audit_event
    add constraint affected_component_enum_check check
        (affected_components <@
         ARRAY ['Record', 'Referral', 'ContributoryFactor', 'SaferCustodyScreeningOutcome', 'Investigation', 'Interview', 'DecisionAndActions', 'Plan', 'IdentifiedNeed', 'Review', 'Attendee']::varchar[]);
