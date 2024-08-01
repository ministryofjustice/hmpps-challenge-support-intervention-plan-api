drop table attendee;
drop table review;

create table review
(
    review_id                     bigserial primary key not null,
    review_uuid                   uuid                  not null unique,
    plan_id                       bigint                not null references plan (plan_id),
    review_sequence               int                   not null,
    review_date                   date,
    recorded_by                   varchar(32)           not null,
    recorded_by_display_name      varchar(255)          not null,
    next_review_date              date,
    actions                       varchar[]             not null,
    csip_closed_date              date,
    summary                       text,
    created_at                    timestamp             not null,
    created_by                    varchar(32)           not null,
    created_by_display_name       varchar(255)          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean               not null default false
);
create index idx_review_record_id on review (plan_id);

alter table review
    add constraint review_actions_enum_check check
        (actions <@
         ARRAY ['ResponsiblePeopleInformed', 'CsipUpdated', 'RemainOnCsip', 'CaseNote', 'CloseCsip']::varchar[]);

create table attendee
(
    attendee_id                   bigserial primary key not null,
    attendee_uuid                 uuid                  not null unique,
    review_id                     bigint                not null references review (review_id),
    name                          varchar(100),
    role                          varchar(50),
    attended                      boolean,
    contribution                  text,
    created_at                    timestamp             not null,
    created_by                    varchar(32)           not null,
    created_by_display_name       varchar(255)          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean               not null
);
create index idx_attendee_review_id on attendee (review_id);