drop table attendee_audit;
drop table review_audit;
drop table identified_need_audit;
drop table interview_audit;
drop table contributory_factor_audit;
drop table plan_audit;
drop table decision_and_actions_audit;
drop table investigation_audit;
drop table safer_custody_screening_outcome_audit;
drop table referral_audit;
drop table csip_record_audit;
drop table audit_revision;
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
drop table csip_record;

create table csip_record
(
    record_id                     uuid         not null primary key,
    prison_number                 varchar(10)  not null,
    prison_code_when_recorded     varchar(6),
    log_code                      varchar(10),
    status                        varchar      not null
        constraint status_enum_check check
            (status in ('CSIP_CLOSED', 'CSIP_OPEN', 'AWAITING_DECISION', 'ACCT_SUPPORT', 'PLAN_PENDING',
                        'INVESTIGATION_PENDING', 'NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP', 'REFERRAL_SUBMITTED',
                        'REFERRAL_PENDING', 'UNKNOWN')),
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_csip_record_prison_number on csip_record (prison_number);

create table referral
(
    referral_id                        uuid         not null primary key references csip_record (record_id),
    incident_date                      date         not null,
    incident_time                      time,
    incident_type_id                   int          not null references reference_data (reference_data_id),
    incident_location_id               int          not null references reference_data (reference_data_id),
    referred_by                        varchar(240) not null,
    referer_area_of_work_id            int          not null references reference_data (reference_data_id),
    referral_date                      date         not null,
    proactive_referral                 boolean,
    staff_assaulted                    boolean,
    assaulted_staff_name               text,
    incident_involvement_id            int references reference_data (reference_data_id),
    description_of_concern             text,
    known_reasons                      text,
    other_information                  text,
    safer_custody_team_informed        varchar(12)
        constraint safer_custody_team_informed_enum_check check
            (safer_custody_team_informed in ('YES', 'NO', 'DO_NOT_KNOW')),
    referral_complete                  boolean,
    referral_completed_by              varchar(64),
    referral_completed_by_display_name varchar(255),
    referral_completed_date            date,
    version                            int          not null,
    created_at                         timestamp    not null,
    created_by                         varchar(64)  not null,
    created_by_display_name            varchar(255) not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(64),
    last_modified_by_display_name      varchar(255)
);
create index idx_referral_incident_type_id on referral (incident_type_id);
create index idx_referral_incident_location_id on referral (incident_location_id);
create index idx_referral_referer_area_of_work_id on referral (referer_area_of_work_id);
create index idx_referral_incident_involvement_id on referral (incident_involvement_id);

create table safer_custody_screening_outcome
(
    safer_custody_screening_outcome_id uuid         not null primary key references referral (referral_id),
    outcome_id                         int          not null references reference_data (reference_data_id),
    recorded_by                        varchar(100) not null,
    recorded_by_display_name           varchar(255) not null,
    date                               date         not null,
    reason_for_decision                text         not null,
    version                            int          not null,
    created_at                         timestamp    not null,
    created_by                         varchar(64)  not null,
    created_by_display_name            varchar(255) not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(64),
    last_modified_by_display_name      varchar(255)
);
create index idx_safer_custody_screening_outcome_outcome_id on safer_custody_screening_outcome (outcome_id);

create table investigation
(
    investigation_id              uuid         not null primary key references referral (referral_id),
    staff_involved                text,
    evidence_secured              text,
    occurrence_reason             text,
    persons_usual_behaviour       text,
    persons_trigger               text,
    protective_factors            text,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    version                       int          not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);

create table decision_and_actions
(
    decision_and_actions_id       uuid         not null primary key references referral (referral_id),
    conclusion                    text,
    outcome_id                    int          not null references reference_data (reference_data_id),
    signed_off_by_role_id         int references reference_data (reference_data_id),
    recorded_by                   varchar(100),
    recorded_by_display_name      varchar(255),
    date                          date,
    next_steps                    text,
    actions                       varchar[]    not null
        constraint actions_enum_check check
            (actions <@
             array ['OPEN_CSIP_ALERT', 'NON_ASSOCIATIONS_UPDATED', 'OBSERVATION_BOOK', 'UNIT_OR_CELL_MOVE', 'CSRA_OR_RSRA_REVIEW', 'SERVICE_REFERRAL', 'SIM_REFERRAL']::varchar[]),
    action_other                  text,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_decision_and_actions_outcome_id on decision_and_actions (outcome_id);
create index idx_decision_and_actions_signed_off_role_id on decision_and_actions (signed_off_by_role_id);

create table plan
(
    plan_id                       uuid         not null primary key references csip_record (record_id),
    case_manager                  varchar(100) not null,
    reason_for_plan               varchar(240) not null,
    first_case_review_date        date         not null,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);

create table contributory_factor
(
    contributory_factor_id        uuid         not null primary key,
    referral_id                   uuid         not null references referral (referral_id),
    contributory_factor_type_id   bigint       not null references reference_data (reference_data_id),
    comment                       text,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_contributory_factor_record_id on contributory_factor (referral_id);
create index idx_contributory_factor_contributory_factor_type_id on contributory_factor (contributory_factor_type_id);

create table interview
(
    interview_id                  uuid         not null primary key,
    investigation_id              uuid         not null references investigation (investigation_id),
    interviewee                   varchar(100) not null,
    interview_date                date         not null,
    interviewee_role_id           int          not null references reference_data (reference_data_id),
    interview_text                text,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_interview_record_id on interview (investigation_id);
create index idx_interview_interviewee_role_id on interview (interviewee_role_id);

create table identified_need
(
    identified_need_id            uuid         not null primary key,
    plan_id                       uuid         not null references plan (plan_id),
    identified_need               text         not null,
    responsible_person            varchar(100) not null,
    created_date                  date         not null,
    target_date                   date         not null,
    closed_date                   date,
    intervention                  text         not null,
    progression                   text,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_identified_need_record_id on identified_need (plan_id);

create table review
(
    review_id                     uuid primary key not null,
    plan_id                       uuid             not null references plan (plan_id),
    review_sequence               int              not null,
    review_date                   date,
    recorded_by                   varchar(64)      not null,
    recorded_by_display_name      varchar(255)     not null,
    next_review_date              date,
    actions                       varchar[]        not null
        constraint review_actions_enum_check check
            (actions <@
             array ['RESPONSIBLE_PEOPLE_INFORMED', 'CSIP_UPDATED', 'REMAIN_ON_CSIP', 'CASE_NOTE', 'CLOSE_CSIP']::varchar[]),
    csip_closed_date              date,
    summary                       text,
    version                       int              not null,
    created_at                    timestamp        not null,
    created_by                    varchar(64)      not null,
    created_by_display_name       varchar(255)     not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255),
    deleted                       boolean          not null default false
);
create index idx_review_record_id on review (plan_id);

create table attendee
(
    attendee_id                   uuid         not null primary key,
    review_id                     uuid         not null references review (review_id),
    name                          varchar(100),
    role                          varchar(50),
    attended                      boolean,
    contribution                  text,
    version                       int          not null,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255)
);
create index idx_attendee_review_id on attendee (review_id);

create table audit_revision
(
    id                  bigserial    not null primary key,
    timestamp           timestamp    not null,
    username            varchar(64)  not null,
    user_display_name   varchar(255) not null,
    caseload_id         varchar(10),
    source              varchar(6)
        constraint check_source check (source in ('DPS', 'NOMIS')),
    affected_components varchar[]
        constraint affected_components_enum_check check
            (affected_components <@
             array ['RECORD', 'REFERRAL', 'CONTRIBUTORY_FACTOR', 'SAFER_CUSTODY_SCREENING_OUTCOME', 'INVESTIGATION', 'INTERVIEW', 'DECISION_AND_ACTIONS', 'PLAN', 'IDENTIFIED_NEED', 'REVIEW', 'ATTENDEE']::varchar[])
);

create table csip_record_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    record_id                     uuid         not null,
    prison_number                 varchar(10)  not null,
    prison_code_when_recorded     varchar(6),
    log_code                      varchar(10),
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255),

    log_code_modified             boolean,
    primary key (rev_id, record_id)
);

create table referral_audit
(
    rev_id                                      bigint       not null references audit_revision (id),
    rev_type                                    smallint     not null,
    referral_id                                 uuid         not null,
    incident_date                               date,
    incident_time                               time,
    incident_type_id                            bigint,
    incident_location_id                        bigint,
    referred_by                                 varchar(240),
    referer_area_of_work_id                     bigint,
    referral_date                               date,
    proactive_referral                          boolean,
    staff_assaulted                             boolean,
    assaulted_staff_name                        text,
    incident_involvement_id                     bigint,
    description_of_concern                      text,
    known_reasons                               text,
    other_information                           text,
    safer_custody_team_informed                 varchar(12),
    referral_complete                           boolean,
    referral_completed_by                       varchar(64),
    referral_completed_by_display_name          varchar(255),
    referral_completed_date                     date,
    created_at                                  timestamp    not null,
    created_by                                  varchar(64)  not null,
    created_by_display_name                     varchar(255) not null,
    last_modified_at                            timestamp,
    last_modified_by                            varchar(64),
    last_modified_by_display_name               varchar(255),

    incident_date_modified                      boolean,
    incident_time_modified                      boolean,
    incident_type_modified                      boolean,
    incident_location_modified                  boolean,
    referred_by_modified                        boolean,
    referer_area_of_work_modified               boolean,
    referral_date_modified                      boolean,
    proactive_referral_modified                 boolean,
    staff_assaulted_modified                    boolean,
    assaulted_staff_name_modified               boolean,
    incident_involvement_modified               boolean,
    description_of_concern_modified             boolean,
    known_reasons_modified                      boolean,
    other_information_modified                  boolean,
    safer_custody_team_informed_modified        boolean,
    referral_complete_modified                  boolean,
    referral_completed_by_modified              boolean,
    referral_completed_by_display_name_modified boolean,
    referral_completed_date_modified            boolean,
    primary key (rev_id, referral_id)
);

create table safer_custody_screening_outcome_audit
(
    rev_id                             bigint       not null references audit_revision (id),
    rev_type                           smallint     not null,
    safer_custody_screening_outcome_id uuid         not null,
    outcome_id                         bigint,
    recorded_by                        varchar(64),
    recorded_by_display_name           varchar(255),
    date                               date,
    reason_for_decision                text,
    created_at                         timestamp    not null,
    created_by                         varchar(64)  not null,
    created_by_display_name            varchar(255) not null,
    last_modified_at                   timestamp,
    last_modified_by                   varchar(64),
    last_modified_by_display_name      varchar(255),

    outcome_modified                   boolean,
    recorded_by_modified               boolean,
    recorded_by_display_name_modified  boolean,
    date_modified                      boolean,
    reason_for_decision_modified       boolean,
    primary key (rev_id, safer_custody_screening_outcome_id)
);

create table investigation_audit
(
    rev_id                           bigint       not null references audit_revision (id),
    rev_type                         smallint     not null,
    investigation_id                 uuid         not null,
    staff_involved                   text,
    evidence_secured                 text,
    occurrence_reason                text,
    persons_usual_behaviour          text,
    persons_trigger                  text,
    protective_factors               text,
    created_at                       timestamp    not null,
    created_by                       varchar(64)  not null,
    created_by_display_name          varchar(255) not null,
    last_modified_at                 timestamp,
    last_modified_by                 varchar(64),
    last_modified_by_display_name    varchar(255),

    staff_involved_modified          boolean,
    evidence_secured_modified        boolean,
    occurrence_reason_modified       boolean,
    persons_usual_behaviour_modified boolean,
    persons_trigger_modified         boolean,
    protective_factors_modified      boolean,
    primary key (rev_id, investigation_id)
);

create table decision_and_actions_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    decision_and_actions_id           uuid         not null,
    conclusion                        text,
    outcome_id                        bigint,
    signed_off_by_role_id             bigint,
    recorded_by                       varchar(64),
    recorded_by_display_name          varchar(255),
    date                              date,
    next_steps                        text,
    actions                           varchar[],
    action_other                      text,
    created_at                        timestamp    not null,
    created_by                        varchar(64)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(64),
    last_modified_by_display_name     varchar(255),

    conclusion_modified               boolean,
    outcome_modified                  boolean,
    signed_off_by_modified            boolean,
    recorded_by_modified              boolean,
    recorded_by_display_name_modified boolean,
    date_modified                     boolean,
    next_steps_modified               boolean,
    actions_modified                  boolean,
    action_other_modified             boolean,
    primary key (rev_id, decision_and_actions_id)
);

create table plan_audit
(
    rev_id                          bigint       not null references audit_revision (id),
    rev_type                        smallint     not null,
    plan_id                         uuid         not null,
    case_manager                    varchar(100),
    reason_for_plan                 varchar(240),
    first_case_review_date          date,
    created_at                      timestamp    not null,
    created_by                      varchar(64)  not null,
    created_by_display_name         varchar(255) not null,
    last_modified_at                timestamp,
    last_modified_by                varchar(64),
    last_modified_by_display_name   varchar(255),

    case_manager_modified           boolean,
    reason_for_plan_modified        boolean,
    first_case_review_date_modified boolean,
    primary key (rev_id, plan_id)
);

create table contributory_factor_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    contributory_factor_id            uuid         not null,
    referral_id                       uuid         not null,
    contributory_factor_type_id       bigint,
    comment                           text,
    created_at                        timestamp    not null,
    created_by                        varchar(64)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(64),
    last_modified_by_display_name     varchar(255),

    contributory_factor_type_modified boolean,
    comment_modified                  boolean,
    primary key (rev_id, contributory_factor_id)
);

create table interview_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    interview_id                  uuid         not null,
    investigation_id              uuid         not null,
    interviewee                   varchar(100),
    interview_date                date,
    interviewee_role_id           bigint,
    interview_text                text,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255),

    interviewee_modified          boolean,
    interview_date_modified       boolean,
    interviewee_role_modified     boolean,
    interview_text_modified       boolean,
    primary key (rev_id, interview_id)
);

create table identified_need_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    identified_need_id            uuid         not null,
    plan_id                       uuid         not null,
    identified_need               text,
    responsible_person            varchar(100),
    created_date                  date,
    target_date                   date,
    closed_date                   date,
    intervention                  text,
    progression                   text,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255),

    identified_need_modified      boolean,
    responsible_person_modified   boolean,
    created_date_modified         boolean,
    target_date_modified          boolean,
    closed_date_modified          boolean,
    intervention_modified         boolean,
    progression_modified          boolean,
    primary key (rev_id, identified_need_id)
);

create table review_audit
(
    rev_id                            bigint       not null references audit_revision (id),
    rev_type                          smallint     not null,
    review_id                         uuid         not null,
    plan_id                           uuid         not null,
    review_sequence                   int,
    review_date                       date,
    recorded_by                       varchar(64),
    recorded_by_display_name          varchar(255),
    next_review_date                  date,
    csip_closed_date                  date,
    summary                           text,
    actions                           varchar[],
    created_at                        timestamp    not null,
    created_by                        varchar(64)  not null,
    created_by_display_name           varchar(255) not null,
    last_modified_at                  timestamp,
    last_modified_by                  varchar(64),
    last_modified_by_display_name     varchar(255),

    review_sequence_modified          boolean,
    review_date_modified              boolean,
    recorded_by_modified              boolean,
    recorded_by_display_name_modified boolean,
    next_review_date_modified         boolean,
    csip_closed_date_modified         boolean,
    summary_modified                  boolean,
    actions_modified                  boolean,
    primary key (rev_id, review_id)
);

create table attendee_audit
(
    rev_id                        bigint       not null references audit_revision (id),
    rev_type                      smallint     not null,
    attendee_id                   uuid         not null,
    review_id                     uuid         not null,
    name                          varchar(100),
    role                          varchar(50),
    attended                      boolean,
    contribution                  text,
    created_at                    timestamp    not null,
    created_by                    varchar(64)  not null,
    created_by_display_name       varchar(255) not null,
    last_modified_at              timestamp,
    last_modified_by              varchar(64),
    last_modified_by_display_name varchar(255),

    name_modified                 boolean,
    role_modified                 boolean,
    attended_modified             boolean,
    contribution_modified         boolean,
    primary key (rev_id, attendee_id)
);

-----

create or replace function actioned_to_close(csip_id uuid) returns boolean as
$$
declare
    all_actions varchar[];
begin
    select array_agg(distinct action)
    into all_actions
    from review,
         unnest(actions) as action
    where plan_id = csip_id
      and actions <> '{}';
    return 'CLOSE_CSIP' = any (all_actions);
end;
$$ language plpgsql;

create or replace function referral_complete(csip_id uuid) returns boolean as
$$
begin
    return exists(select 1
                  from referral ref
                  where ref.referral_id = csip_id
                    and ref.referral_complete = true);
end;
$$ language plpgsql;

create or replace function is_acc_support(screening_outcome varchar, decision_outcome varchar) returns boolean as
$$
begin
    return screening_outcome = 'ACC' or (screening_outcome = 'OPE' and decision_outcome = 'ACC');
end;
$$ language plpgsql;

create or replace function is_plan_pending(screening_outcome varchar, decision_outcome varchar) returns boolean as
$$
begin
    return screening_outcome = 'CUR' or (screening_outcome = 'OPE' and decision_outcome = 'CUR');
end;
$$ language plpgsql;

create or replace function is_no_further_action(screening_outcome varchar, decision_outcome varchar) returns boolean as
$$
begin
    return screening_outcome = 'NFA' or (screening_outcome = 'OPE' and decision_outcome = 'NFA');
end;
$$ language plpgsql;

create or replace function is_outside_support(screening_outcome varchar, decision_outcome varchar) returns boolean as
$$
begin
    return screening_outcome = 'WIN' or (screening_outcome = 'OPE' and decision_outcome = 'WIN');
end;
$$ language plpgsql;

create or replace function calculate_csip_status(csip_id uuid) returns varchar as
$$
declare
    actioned_to_close  boolean;
    referral_complete  boolean;
    screening_outcome  varchar;
    decision_outcome   varchar;
    investigation_made boolean;
begin
    actioned_to_close = actioned_to_close(csip_id);
    if actioned_to_close then
        return 'CSIP_CLOSED';
    elsif exists(select 1 from plan where plan_id = csip_id) then
        return 'CSIP_OPEN';
    end if;

    referral_complete = referral_complete(csip_id);
    if not referral_complete then
        return 'REFERRAL_PENDING';
    end if;

    select sout.code,
           dout.code,
           coalesce(inv.staff_involved, inv.evidence_secured, inv.occurrence_reason, inv.persons_usual_behaviour,
                    inv.persons_trigger, inv.protective_factors) is not null
    into screening_outcome, decision_outcome, investigation_made
    from referral r
             join safer_custody_screening_outcome scso on scso.safer_custody_screening_outcome_id = r.referral_id
             join reference_data sout on sout.reference_data_id = scso.outcome_id
             left join decision_and_actions da on da.decision_and_actions_id = r.referral_id
             left join reference_data dout on dout.reference_data_id = da.outcome_id
             left join investigation inv on inv.investigation_id = r.referral_id
    where r.referral_id = csip_id;

    return case
               when is_plan_pending(screening_outcome, decision_outcome) then 'PLAN_PENDING'
               when screening_outcome = 'OPE' and decision_outcome is null and investigation_made then 'AWAITING_DECISION'
               when screening_outcome = 'OPE' and decision_outcome is null then 'INVESTIGATION_PENDING'
               when is_no_further_action(screening_outcome, decision_outcome) then 'NO_FURTHER_ACTION'
               when is_outside_support(screening_outcome, decision_outcome) then 'SUPPORT_OUTSIDE_CSIP'
               when is_acc_support(screening_outcome, decision_outcome) then 'ACCT_SUPPORT'
               when screening_outcome is null then 'REFERRAL_SUBMITTED'
               else 'UNKNOWN'
        end;

end;
$$ language plpgsql;

create or replace function update_csip_status(csip_id uuid) returns void as
$$
begin
    update csip_record set status = calculate_csip_status(csip_id) where record_id = csip_id;
end;
$$ language plpgsql;

-----

create or replace function set_status_on_csip() returns trigger as
$$
begin
    new.status := calculate_csip_status(new.record_id);
    return new;
end;
$$ language plpgsql;

create or replace trigger csip_record_insert_csip_status
    before insert
    on csip_record
    for each row
execute function set_status_on_csip();

create or replace function update_status_from_referral() returns trigger as
$$
begin
    perform update_csip_status(coalesce(new.referral_id, old.referral_id));
    return coalesce(new, old);
end;
$$ language plpgsql;

create or replace trigger referral_insert_csip_status
    after insert
    on referral
    for each row
execute function update_status_from_referral();

create or replace trigger referral_update_csip_status
    after update
    on referral
    for each row
    when (old.referral_complete is distinct from new.referral_complete)
execute function update_status_from_referral();

create or replace trigger referral_delete_csip_status
    after delete
    on referral
    for each row
execute function update_status_from_referral();

create or replace function update_status_from_scso() returns trigger as
$$
begin
    perform update_csip_status(coalesce(new.safer_custody_screening_outcome_id,
                                        old.safer_custody_screening_outcome_id));
    return coalesce(new, old);
end;
$$ language plpgsql;

create or replace trigger scso_insert_csip_status
    after insert
    on safer_custody_screening_outcome
    for each row
execute function update_status_from_scso();

create or replace trigger scso_update_csip_status
    after update
    on safer_custody_screening_outcome
    for each row
    when (old.outcome_id is distinct from new.outcome_id)
execute function update_status_from_scso();

create or replace trigger scso_delete_csip_status
    after delete
    on safer_custody_screening_outcome
    for each row
execute function update_status_from_scso();

create or replace function update_status_from_decision() returns trigger as
$$
begin
    perform update_csip_status(coalesce(new.decision_and_actions_id, old.decision_and_actions_id));
    return coalesce(new, old);
end;
$$ language plpgsql;

create or replace trigger decision_actions_insert_csip_status
    after insert
    on decision_and_actions
    for each row
execute function update_status_from_decision();

create or replace trigger decision_actions_update_csip_status
    after update
    on decision_and_actions
    for each row
    when (old.outcome_id is distinct from new.outcome_id)
execute function update_status_from_decision();

create or replace trigger decision_actions_delete_csip_status
    after delete
    on decision_and_actions
    for each row
execute function update_status_from_decision();

create or replace function update_status_from_investigation() returns trigger as
$$
begin
    perform update_csip_status(coalesce(new.investigation_id, old.investigation_id));
    return coalesce(new, old);
end;
$$ language plpgsql;

create or replace trigger investigation_insert_csip_status
    after insert
    on investigation
    for each row
execute function update_status_from_investigation();

create or replace trigger investigation_update_csip_status
    after update
    on investigation
    for each row
    when (old is distinct from new)
execute function update_status_from_investigation();

create or replace trigger investigation_delete_csip_status
    after delete
    on investigation
    for each row
execute function update_status_from_investigation();

create or replace function update_status_from_plan() returns trigger as
$$
begin
    perform update_csip_status(coalesce(new.plan_id, old.plan_id));
    return coalesce(new, old);
end;
$$ language plpgsql;

create or replace trigger plan_insert_csip_status
    after insert
    on plan
    for each row
execute function update_status_from_plan();

create or replace trigger plan_delete_csip_status
    after delete
    on plan
    for each row
execute function update_status_from_plan();

create or replace trigger review_insert_csip_status
    after insert
    on review
    for each row
execute function update_status_from_plan();

create or replace trigger review_update_csip_status
    after update
    on review
    for each row
    when (old.actions is distinct from new.actions)
execute function update_status_from_plan();

create or replace trigger review_delete_csip_status
    after delete
    on review
    for each row
execute function update_status_from_plan();