truncate csip_record cascade;
drop table attendee;
drop table review;
drop table identified_need;
drop table interview;
drop table contributory_factor;
drop table plan;
drop table decision_and_actions;
drop table investigation;
drop table safer_custody_screening_outcome;
drop table referral;

alter table csip_record
    add column deleted boolean not null default false;

create table referral
(
    referral_id                        bigint primary key not null references csip_record (record_id),
    incident_date                      date               not null,
    incident_time                      time,
    incident_type_id                   int                not null references reference_data (reference_data_id),
    incident_location_id               int                not null references reference_data (reference_data_id),
    referred_by                        varchar(240)       not null,
    referer_area_of_work_id            int                not null references reference_data (reference_data_id),
    referral_date                      date               not null,
    referral_summary                   text,
    proactive_referral                 boolean,
    staff_assaulted                    boolean,
    assaulted_staff_name               text,
    release_date                       date,
    incident_involvement_id            int references reference_data (reference_data_id),
    description_of_concern             text,
    known_reasons                      text,
    other_information                  text,
    safer_custody_team_informed        varchar(12),
    referral_complete                  boolean,
    referral_completed_by              varchar(32),
    referral_completed_by_display_name varchar(255),
    referral_completed_date            date,
    created_at                         timestamp          not null,
    created_by                         varchar(32)        not null,
    created_by_display_name            varchar(255)       not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(32),
    last_modified_by_display_name      varchar(255),
    deleted                            boolean            not null default false
);
create index idx_referral_incident_type_id on referral (incident_type_id);
create index idx_referral_incident_location_id on referral (incident_location_id);
create index idx_referral_referer_area_of_work_id on referral (referer_area_of_work_id);
create index idx_referral_incident_involvement_id on referral (incident_involvement_id);

alter table referral
    add constraint safer_custody_team_informed_enum_check check
        (safer_custody_team_informed in ('YES', 'NO', 'DO_NOT_KNOW'));

create table safer_custody_screening_outcome
(
    safer_custody_screening_outcome_id bigint primary key not null references referral (referral_id),
    outcome_id                         int                not null references reference_data (reference_data_id),
    recorded_by                        varchar(100)       not null,
    recorded_by_display_name           varchar(255)       not null,
    date                               date               not null,
    reason_for_decision                text               not null,
    created_at                         timestamp          not null,
    created_by                         varchar(32)        not null,
    created_by_display_name            varchar(255)       not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(32),
    last_modified_by_display_name      varchar(255),
    deleted                            boolean            not null default false
);
create index idx_safer_custody_screening_outcome_outcome_id on safer_custody_screening_outcome (outcome_id);

create table investigation
(
    investigation_id              bigint primary key not null references referral (referral_id),
    staff_involved                text,
    evidence_secured              text,
    occurrence_reason             text,
    persons_usual_behaviour       text,
    persons_trigger               text,
    protective_factors            text,
    created_at                    timestamp          not null,
    created_by                    varchar(32)        not null,
    created_by_display_name       varchar(255)       not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean            not null
);

create table decision_and_actions
(
    decision_and_actions_id       bigint primary key not null references referral (referral_id),
    conclusion                    text,
    outcome_id                    int                not null references reference_data (reference_data_id),
    signed_off_by_role_id         int references reference_data (reference_data_id),
    recorded_by                   varchar(100),
    recorded_by_display_name      varchar(255),
    date                          date,
    next_steps                    text,
    actions                       varchar[]          not null,
    action_other                  text,
    created_at                    timestamp          not null,
    created_by                    varchar(32)        not null,
    created_by_display_name       varchar(255)       not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean            not null
);
create index idx_decision_and_actions_outcome_id on decision_and_actions (outcome_id);
create index idx_decision_and_actions_signed_off_role_id on decision_and_actions (signed_off_by_role_id);

alter table decision_and_actions
    add constraint actions_enum_check check
        (actions <@
         ARRAY ['OpenCsipAlert', 'NonAssociationsUpdated', 'ObservationBook', 'UnitOrCellMove', 'CsraOrRsraReview', 'ServiceReferral', 'SimReferral']::varchar[]);

create table plan
(
    plan_id                       bigint primary key not null references csip_record (record_id),
    case_manager                  varchar(100)       not null,
    reason_for_plan               varchar(240)       not null,
    first_case_review_date        date               not null,
    created_at                    timestamp          not null,
    created_by                    varchar(32)        not null,
    created_by_display_name       varchar(255)       not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean            not null
);

create table contributory_factor
(
    contributory_factor_id        bigserial primary key not null,
    contributory_factor_uuid      uuid                  not null unique,
    referral_id                   bigserial             not null references referral (referral_id),
    contributory_factor_type_id   bigserial             not null references reference_data (reference_data_id),
    comment                       text,
    created_at                    timestamp             not null,
    created_by                    varchar(32)           not null,
    created_by_display_name       varchar(255)          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean               not null
);
create index idx_contributory_factor_record_id on contributory_factor (referral_id);
create index idx_contributory_factor_contributory_factor_type_id on contributory_factor (contributory_factor_type_id);

create table interview
(
    interview_id                  bigserial primary key not null,
    interview_uuid                uuid                  not null unique,
    investigation_id              bigserial             not null references investigation (investigation_id),
    interviewee                   varchar(100)          not null,
    interview_date                date                  not null,
    interviewee_role_id           int                   not null references reference_data (reference_data_id),
    interview_text                text,
    created_at                    timestamp             not null,
    created_by                    varchar(32)           not null,
    created_by_display_name       varchar(255)          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean               not null
);
create index idx_interview_record_id on interview (investigation_id);
create index idx_interview_interviewee_role_id on interview (interviewee_role_id);

create table identified_need
(
    identified_need_id            bigserial primary key not null,
    identified_need_uuid          uuid                  not null unique,
    plan_id                       bigserial             not null references plan (plan_id),
    identified_need               text                  not null,
    need_identified_by            varchar(100)          not null,
    created_date                  date                  not null,
    target_date                   date                  not null,
    closed_date                   date,
    intervention                  text                  not null,
    progression                   text,
    created_at                    timestamp             not null,
    created_by                    varchar(32)           not null,
    created_by_display_name       varchar(255)          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),
    deleted                       boolean               not null
);
create index idx_identified_need_record_id on identified_need (plan_id);

create table review
(
    review_id                          bigserial primary key not null,
    review_uuid                        uuid                  not null unique,
    plan_id                            bigserial             not null references plan (plan_id),
    review_sequence                    int                   not null,
    review_date                        date,
    recorded_by                        varchar(32)           not null,
    recorded_by_display_name           varchar(255)          not null,
    next_review_date                   date,
    action_responsible_people_informed boolean,
    action_csip_updated                boolean,
    action_remain_on_csip              boolean,
    action_case_note                   boolean,
    action_close_csip                  boolean,
    csip_closed_date                   date,
    summary                            text,
    created_at                         timestamp             not null,
    created_by                         varchar(32)           not null,
    created_by_display_name            varchar(255)          not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(32),
    last_modified_by_display_name      varchar(255),
    deleted                            boolean               not null default false
);

create table attendee
(
    attendee_id                   bigserial primary key not null,
    attendee_uuid                 uuid                  not null unique,
    review_id                     bigserial             not null references review (review_id),
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
create index idx_review_record_id on review (plan_id);
create index idx_attendee_review_id on attendee (review_id);