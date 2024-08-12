create or replace function actioned_to_close(csip_id bigint) returns boolean as
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

truncate table audit_revision cascade;

alter table audit_revision
    add constraint affected_component_enum_check check
        (affected_components <@
         ARRAY ['RECORD', 'REFERRAL', 'CONTRIBUTORY_FACTOR', 'SAFER_CUSTODY_SCREENING_OUTCOME', 'INVESTIGATION', 'INTERVIEW', 'DECISION_AND_ACTIONS', 'PLAN', 'IDENTIFIED_NEED', 'REVIEW', 'ATTENDEE']::varchar[]);

alter table decision_and_actions
    drop constraint actions_enum_check;
alter table decision_and_actions
    add constraint decision_actions_enum_check check
        (actions <@
         ARRAY ['OPEN_CSIP_ALERT', 'NON_ASSOCIATIONS_UPDATED', 'OBSERVATION_BOOK', 'UNIT_OR_CELL_MOVE', 'CSRA_OR_RSRA_REVIEW', 'SERVICE_REFERRAL', 'SIM_REFERRAL']::varchar[]);

alter table review
    drop constraint review_actions_enum_check;
alter table review
    add constraint review_actions_enum_check check
        (actions <@
         ARRAY ['RESPONSIBLE_PEOPLE_INFORMED', 'CSIP_UPDATED', 'REMAIN_ON_CSIP', 'CASE_NOTE', 'CLOSE_CSIP']::varchar[]);