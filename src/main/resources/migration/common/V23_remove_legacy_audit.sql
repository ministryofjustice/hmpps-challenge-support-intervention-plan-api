drop table audit_event;

drop table attendee_audit;
drop table review_audit;
drop table identified_need_audit;
drop table plan_audit;
drop table interview_audit;
drop table investigation_audit;
drop table decision_and_actions_audit;
drop table safer_custody_screening_outcome_audit;
drop table contributory_factor_audit;
drop table referral_audit;
drop table csip_record_audit;
drop table audit_revision;

create table audit_revision
(
    id                  bigserial    not null primary key,
    timestamp           timestamp    not null,
    username            varchar(32)  not null,
    user_display_name   varchar(255) not null,
    caseload_id         varchar(10),
    source              varchar(6)
        constraint check_source check (source in ('DPS', 'NOMIS')),
    affected_components varchar[]
);

create table csip_record_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    record_id                     bigint       not null,
    record_uuid                   uuid         not null,
    prison_number                 varchar(10)  not null,
    prison_code_when_recorded     varchar(6),
    log_code                      varchar(10),
    created_at                    timestamp    not null,
    created_by                    varchar(32)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    log_code_modified             boolean      not null,
    primary key (rev_id, record_id)
);

create table referral_audit
(
    rev_id                                      bigint       not null references audit_revision (id),
    rev_type                                    smallint     not null,
    referral_id                                 bigint       not null,
    incident_date                               date,
    incident_time                               time,
    incident_type_id                            bigint references reference_data (reference_data_id),
    incident_location_id                        bigint references reference_data (reference_data_id),
    referred_by                                 varchar(240),
    referer_area_of_work_id                     bigint references reference_data (reference_data_id),
    referral_date                               date,
    referral_summary                            text,
    proactive_referral                          boolean,
    staff_assaulted                             boolean,
    assaulted_staff_name                        text,
    release_date                                date,
    incident_involvement_id                     bigint references reference_data (reference_data_id),
    description_of_concern                      text,
    known_reasons                               text,
    other_information                           text,
    safer_custody_team_informed                 varchar(12),
    referral_complete                           boolean,
    referral_completed_by                       varchar(32),
    referral_completed_by_display_name          varchar(255),
    referral_completed_date                     date,
    created_at                                  timestamp    not null,
    created_by                                  varchar(32)  not null,
    created_by_display_name                     varchar(255) not null,
    last_modified_at                            timestamp,
    last_modified_by                            varchar(32),
    last_modified_by_display_name               varchar(255),

    incident_date_modified                      boolean      not null,
    incident_time_modified                      boolean      not null,
    incident_type_modified                      boolean      not null,
    incident_location_modified                  boolean      not null,
    referred_by_modified                        boolean      not null,
    referer_area_of_work_modified               boolean      not null,
    referral_date_modified                      boolean      not null,
    referral_summary_modified                   boolean      not null,
    proactive_referral_modified                 boolean      not null,
    staff_assaulted_modified                    boolean      not null,
    assaulted_staff_name_modified               boolean      not null,
    release_date_modified                       boolean      not null,
    incident_involvement_modified               boolean      not null,
    description_of_concern_modified             boolean      not null,
    known_reasons_modified                      boolean      not null,
    other_information_modified                  boolean      not null,
    safer_custody_team_informed_modified        boolean      not null,
    referral_complete_modified                  boolean      not null,
    referral_completed_by_modified              boolean      not null,
    referral_completed_by_display_name_modified boolean      not null,
    referral_completed_date_modified            boolean      not null,
    primary key (rev_id, referral_id)
);

create table safer_custody_screening_outcome_audit
(
    rev_id                             bigint       not null references audit_revision (id),
    rev_type                           smallint     not null,
    safer_custody_screening_outcome_id bigint       not null references reference_data (reference_data_id),
    outcome_id                         bigint references reference_data (reference_data_id),
    recorded_by                        varchar(100),
    recorded_by_display_name           varchar(255),
    date                               date,
    reason_for_decision                text,
    created_at                         timestamp    not null,
    created_by                         varchar(32)  not null,
    created_by_display_name            varchar(255) not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(32),
    last_modified_by_display_name      varchar(255),

    outcome_modified                   boolean      not null,
    recorded_by_modified               boolean      not null,
    recorded_by_display_name_modified  boolean      not null,
    date_modified                      boolean      not null,
    reason_for_decision_modified       boolean      not null,
    primary key (rev_id, safer_custody_screening_outcome_id)
);

create table investigation_audit
(
    rev_id                           bigint       not null references audit_revision (id),
    rev_type                         smallint     not null,
    investigation_id                 bigint       not null,
    staff_involved                   text,
    evidence_secured                 text,
    occurrence_reason                text,
    persons_usual_behaviour          text,
    persons_trigger                  text,
    protective_factors               text,
    created_at                       timestamp    not null,
    created_by                       varchar(32)  not null,
    created_by_display_name          varchar(255) not null,
    last_modified_at                 timestamp,
    last_modified_by                 varchar(32),
    last_modified_by_display_name    varchar(255),

    staff_involved_modified          boolean      not null,
    evidence_secured_modified        boolean      not null,
    occurrence_reason_modified       boolean      not null,
    persons_usual_behaviour_modified boolean      not null,
    persons_trigger_modified         boolean      not null,
    protective_factors_modified      boolean      not null,
    primary key (rev_id, investigation_id)
);

create table decision_and_actions_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    decision_and_actions_id           bigint       not null,
    conclusion                        text,
    outcome_id                        bigint references reference_data (reference_data_id),
    signed_off_by_role_id             bigint references reference_data (reference_data_id),
    recorded_by                       varchar(100),
    recorded_by_display_name          varchar(255),
    date                              date,
    next_steps                        text,
    actions                           varchar[],
    action_other                      text,
    created_at                        timestamp    not null,
    created_by                        varchar(32)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    conclusion_modified               boolean      not null,
    outcome_modified                  boolean      not null,
    signed_off_by_modified            boolean      not null,
    recorded_by_modified              boolean      not null,
    recorded_by_display_name_modified boolean      not null,
    date_modified                     boolean      not null,
    next_steps_modified               boolean      not null,
    actions_modified                  boolean      not null,
    action_other_modified             boolean      not null,
    primary key (rev_id, decision_and_actions_id)
);

create table plan_audit
(
    rev_id                          bigint       not null references audit_revision (id),
    rev_type                        smallint     not null,
    plan_id                         bigint       not null,
    case_manager                    varchar(100),
    reason_for_plan                 varchar(240),
    first_case_review_date          date,
    created_at                      timestamp    not null,
    created_by                      varchar(32)  not null,
    created_by_display_name         varchar(255) not null,
    last_modified_at                timestamp,
    last_modified_by                varchar(32),
    last_modified_by_display_name   varchar(255),

    case_manager_modified           boolean      not null,
    reason_for_plan_modified        boolean      not null,
    first_case_review_date_modified boolean      not null,
    primary key (rev_id, plan_id)
);

create table contributory_factor_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    contributory_factor_id            bigint       not null,
    contributory_factor_uuid          uuid         not null,
    referral_id                       bigint       not null,
    contributory_factor_type_id       bigint references reference_data (reference_data_id),
    comment                           text,
    created_at                        timestamp    not null,
    created_by                        varchar(32)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    contributory_factor_type_modified boolean      not null,
    comment_modified                  boolean      not null,
    primary key (rev_id, contributory_factor_id)
);

create table interview_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    interview_id                  bigint       not null,
    interview_uuid                uuid         not null,
    investigation_id              bigint       not null,
    interviewee                   varchar(100),
    interview_date                date,
    interviewee_role_id           bigint references reference_data (reference_data_id),
    interview_text                text,
    created_at                    timestamp    not null,
    created_by                    varchar(32)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    interviewee_modified          boolean      not null,
    interview_date_modified       boolean      not null,
    interviewee_role_modified     boolean      not null,
    interview_text_modified       boolean      not null,
    primary key (rev_id, interview_id)
);

create table identified_need_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    identified_need_id            bigint       not null,
    identified_need_uuid          uuid         not null,
    plan_id                       bigint       not null,
    identified_need               text,
    responsible_person            varchar(100),
    created_date                  date,
    target_date                   date,
    closed_date                   date,
    intervention                  text,
    progression                   text,
    created_at                    timestamp    not null,
    created_by                    varchar(32)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    identified_need_modified      boolean      not null,
    responsible_person_modified   boolean      not null,
    created_date_modified         boolean      not null,
    target_date_modified          boolean      not null,
    closed_date_modified          boolean      not null,
    intervention_modified         boolean      not null,
    progression_modified          boolean      not null,
    primary key (rev_id, identified_need_id)
);

create table review_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    review_id                         bigint       not null,
    review_uuid                       uuid         not null,
    plan_id                           bigint       not null,
    review_sequence                   int,
    review_date                       date,
    recorded_by                       varchar(32),
    recorded_by_display_name          varchar(255),
    next_review_date                  date,
    csip_closed_date                  date,
    summary                           text,
    actions                           varchar[],
    created_at                        timestamp    not null,
    created_by                        varchar(32)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    review_sequence_modified          boolean      not null,
    review_date_modified              boolean      not null,
    recorded_by_modified              boolean      not null,
    recorded_by_display_name_modified boolean      not null,
    next_review_date_modified         boolean      not null,
    csip_closed_date_modified         boolean      not null,
    summary_modified                  boolean      not null,
    actions_modified                  boolean      not null,
    primary key (rev_id, review_id)
);

create table attendee_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    attendee_id                   bigint       not null,
    attendee_uuid                 uuid         not null,
    review_id                     bigint       not null,
    name                          varchar(100),
    role                          varchar(50),
    attended                      boolean,
    contribution                  text,
    created_at                    timestamp    not null,
    created_by                    varchar(32)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    name_modified                 boolean      not null,
    role_modified                 boolean      not null,
    attended_modified             boolean      not null,
    contribution_modified         boolean      not null,
    primary key (rev_id, attendee_id)
);