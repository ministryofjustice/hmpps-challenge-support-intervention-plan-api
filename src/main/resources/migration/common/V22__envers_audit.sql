create table audit_revision
(
    id                    bigserial    not null primary key,
    datetime              timestamp    not null,
    username              varchar(32) not null,
    user_display_name     varchar(255) not null,
    caseload_id           varchar(10),
    source                varchar(6)
        constraint check_source check (source in ('DPS', 'NOMIS')),
    affected_components varchar[]
);

create table csip_record_audit
(
    rev_id                        bigint,
    rev_type                      smallint,
    record_id                     bigint,
    record_uuid                   uuid,
    prison_number                 varchar(10),
    prison_code_when_recorded     varchar(6),
    log_code                      varchar(10),
    created_at                    timestamp,
    created_by                    varchar(32),
    created_by_display_name       varchar(255),
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    log_code_modified             boolean
);

create table referral_audit
(
    rev_id                                      bigint,
    rev_type                                    smallint,
    referral_id                                 bigint,
    incident_date                               date,
    incident_time                               time,
    incident_type_id                            bigint,
    incident_location_id                        bigint,
    referred_by                                 varchar(240),
    referer_area_of_work_id                     bigint,
    referral_date                               date,
    referral_summary                            text,
    proactive_referral                          boolean,
    staff_assaulted                             boolean,
    assaulted_staff_name                        text,
    release_date                                date,
    incident_involvement_id                     bigint,
    description_of_concern                      text,
    known_reasons                               text,
    other_information                           text,
    safer_custody_team_informed                 varchar(12),
    referral_complete                           boolean,
    referral_completed_by                       varchar(32),
    referral_completed_by_display_name          varchar(255),
    referral_completed_date                     date,
    created_at                                  timestamp,
    created_by                                  varchar(32),
    created_by_display_name                     varchar(255),
    last_modified_at                            timestamp,
    last_modified_by                            varchar(32),
    last_modified_by_display_name               varchar(255),

    incident_date_modified                      boolean,
    incident_time_modified                      boolean,
    incident_type_modified                      boolean,
    incident_location_modified                  boolean,
    referred_by_modified                        boolean,
    referer_area_of_work_modified               boolean,
    referral_date_modified                      boolean,
    referral_summary_modified                   boolean,
    proactive_referral_modified                 boolean,
    staff_assaulted_modified                    boolean,
    assaulted_staff_name_modified               boolean,
    release_date_modified                       boolean,
    incident_involvement_modified               boolean,
    description_of_concern_modified             boolean,
    known_reasons_modified                      boolean,
    other_information_modified                  boolean,
    safer_custody_team_informed_modified        boolean,
    referral_complete_modified                  boolean,
    referral_completed_by_modified              boolean,
    referral_completed_by_display_name_modified boolean,
    referral_completed_date_modified            boolean
);

create table safer_custody_screening_outcome_audit
(
    rev_id                             bigint,
    rev_type                           smallint,
    safer_custody_screening_outcome_id bigint,
    outcome_id                         bigint,
    recorded_by                        varchar(100),
    recorded_by_display_name           varchar(255),
    date                               date,
    reason_for_decision                text,
    created_at                         timestamp,
    created_by                         varchar(32),
    created_by_display_name            varchar(255),
    last_modified_at                   timestamp,
    last_modified_by                   varchar(32),
    last_modified_by_display_name      varchar(255),

    outcome_modified                   boolean,
    recorded_by_modified               boolean,
    recorded_by_display_name_modified  boolean,
    date_modified                      boolean,
    reason_for_decision_modified       boolean
);

create table investigation_audit
(
    rev_id                           bigint,
    rev_type                         smallint,
    investigation_id                 bigint,
    staff_involved                   text,
    evidence_secured                 text,
    occurrence_reason                text,
    persons_usual_behaviour          text,
    persons_trigger                  text,
    protective_factors               text,
    created_at                       timestamp,
    created_by                       varchar(32),
    created_by_display_name          varchar(255),
    last_modified_at                 timestamp,
    last_modified_by                 varchar(32),
    last_modified_by_display_name    varchar(255),

    staff_involved_modified          boolean,
    evidence_secured_modified        boolean,
    occurrence_reason_modified       boolean,
    persons_usual_behaviour_modified boolean,
    persons_trigger_modified         boolean,
    protective_factors_modified      boolean
);

create table decision_and_actions_audit
(
    rev_id                            bigint,
    rev_type                          smallint,
    decision_and_actions_id           bigint,
    conclusion                        text,
    outcome_id                        bigint,
    signed_off_by_role_id             bigint,
    recorded_by                       varchar(100),
    recorded_by_display_name          varchar(255),
    date                              date,
    next_steps                        text,
    actions                           varchar[],
    action_other                      text,
    created_at                        timestamp,
    created_by                        varchar(32),
    created_by_display_name           varchar(255),
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    conclusion_modified               boolean,
    outcome_modified                  boolean,
    signed_off_by_modified            boolean,
    recorded_by_modified              boolean,
    recorded_by_display_name_modified boolean,
    date_modified                     boolean,
    next_steps_modified               boolean,
    actions_modified                  boolean,
    action_other_modified             boolean
);

create table plan_audit
(
    rev_id                          bigint,
    rev_type                        smallint,
    plan_id                         bigint,
    case_manager                    varchar(100),
    reason_for_plan                 varchar(240),
    first_case_review_date          date,
    created_at                      timestamp,
    created_by                      varchar(32),
    created_by_display_name         varchar(255),
    last_modified_at                timestamp,
    last_modified_by                varchar(32),
    last_modified_by_display_name   varchar(255),

    case_manager_modified           boolean,
    reason_for_plan_modified        boolean,
    first_case_review_date_modified boolean
);

create table contributory_factor_audit
(
    rev_id                            bigint,
    rev_type                          smallint,
    contributory_factor_id            bigint,
    contributory_factor_uuid          uuid,
    referral_id                       bigint,
    contributory_factor_type_id       bigint,
    comment                           text,
    created_at                        timestamp,
    created_by                        varchar(32),
    created_by_display_name           varchar(255),
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    contributory_factor_type_modified boolean,
    comment_modified                  boolean
);

create table interview_audit
(
    rev_id                        bigint,
    rev_type                      smallint,
    interview_id                  bigint,
    interview_uuid                uuid,
    investigation_id              bigint,
    interviewee                   varchar(100),
    interview_date                date,
    interviewee_role_id           bigint,
    interview_text                text,
    created_at                    timestamp,
    created_by                    varchar(32),
    created_by_display_name       varchar(255),
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    interviewee_modified          boolean,
    interview_date_modified       boolean,
    interviewee_role_modified     boolean,
    interview_text_modified       boolean
);

create table identified_need_audit
(
    rev_id                        bigint,
    rev_type                      smallint,
    identified_need_id            bigint,
    identified_need_uuid          uuid,
    plan_id                       bigint,
    identified_need               text,
    responsible_person            varchar(100),
    created_date                  date,
    target_date                   date,
    closed_date                   date,
    intervention                  text,
    progression                   text,
    created_at                    timestamp,
    created_by                    varchar(32),
    created_by_display_name       varchar(255),
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    identified_need_modified      boolean,
    responsible_person_modified   boolean,
    created_date_modified         boolean,
    target_date_modified          boolean,
    closed_date_modified          boolean,
    intervention_modified         boolean,
    progression_modified          boolean
);

create table review_audit
(
    rev_id                            bigint,
    rev_type                          smallint,
    review_id                         bigint,
    review_uuid                       uuid,
    plan_id                           bigint,
    review_sequence                   int,
    review_date                       date,
    recorded_by                       varchar(32),
    recorded_by_display_name          varchar(255),
    next_review_date                  date,
    csip_closed_date                  date,
    summary                           text,
    actions                           varchar[],
    created_at                        timestamp,
    created_by                        varchar(32),
    created_by_display_name           varchar(255),
    last_modified_at                  timestamp,
    last_modified_by                  varchar(32),
    last_modified_by_display_name     varchar(255),

    review_sequence_modified          boolean,
    review_date_modified              boolean,
    recorded_by_modified              boolean,
    recorded_by_display_name_modified boolean,
    next_review_date_modified         boolean,
    csip_closed_date_modified         boolean,
    summary_modified                  boolean,
    actions_modified                  boolean
);

create table attendee_audit
(
    rev_id                        bigint,
    rev_type                      smallint,
    attendee_id                   bigint,
    attendee_uuid                 uuid,
    review_id                     bigint,
    name                          varchar(100),
    role                          varchar(50),
    attended                      boolean,
    contribution                  text,
    created_at                    timestamp,
    created_by                    varchar(32),
    created_by_display_name       varchar(255),
    last_modified_at              timestamp,
    last_modified_by              varchar(32),
    last_modified_by_display_name varchar(255),

    name_modified                 boolean,
    role_modified                 boolean,
    attended_modified             boolean,
    contribution_modified         boolean
);