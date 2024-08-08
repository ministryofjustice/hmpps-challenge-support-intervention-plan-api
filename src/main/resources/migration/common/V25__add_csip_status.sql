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
    return 'CloseCsip' = any (all_actions);
end;
$$ language plpgsql;

create or replace function referral_complete(csip_id bigint) returns boolean as
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

create or replace function calculate_csip_status(csip_id bigint) returns varchar as
$$
declare
    actioned_to_close   boolean;
    referral_complete   boolean;
    screening_outcome   varchar;
    decision_outcome    varchar;
    decision_conclusion varchar;
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

    select sout.code, dout.code, da.conclusion
    into screening_outcome, decision_outcome, decision_conclusion
    from referral r
             join safer_custody_screening_outcome scso on scso.safer_custody_screening_outcome_id = r.referral_id
             join reference_data sout on sout.reference_data_id = scso.outcome_id
             left join decision_and_actions da on da.decision_and_actions_id = r.referral_id
             left join reference_data dout on dout.reference_data_id = da.outcome_id
    where r.referral_id = csip_id;

    return case
               when decision_conclusion is null and decision_outcome is not null and screening_outcome = 'OPE'
                   then 'AWAITING_DECISION'
               when is_acc_support(screening_outcome, decision_outcome) then 'ACCT_SUPPORT'
               when is_plan_pending(screening_outcome, decision_outcome) then 'PLAN_PENDING'
               when screening_outcome = 'OPE' and decision_outcome is null then 'INVESTIGATION_PENDING'
               when is_no_further_action(screening_outcome, decision_outcome) then 'NO_FURTHER_ACTION'
               when is_outside_support(screening_outcome, decision_outcome) then 'SUPPORT_OUTSIDE_CSIP'
               when screening_outcome is null then 'REFERRAL_SUBMITTED'
               else 'UNKNOWN'
        end;

end;
$$ language plpgsql;

create or replace function update_csip_status(csip_id bigint) returns void as
$$
begin
    update csip_record set status = calculate_csip_status(csip_id) where record_id = csip_id;
end;
$$ language plpgsql;

-----

alter table csip_record
    add column status varchar;

select update_csip_status(record_id)
from csip_record;

alter table csip_record
    alter status set not null,
    add constraint status_enum_check check
        (status in ('CSIP_CLOSED',
                    'CSIP_OPEN',
                    'AWAITING_DECISION',
                    'ACCT_SUPPORT',
                    'PLAN_PENDING',
                    'INVESTIGATION_PENDING',
                    'NO_FURTHER_ACTION',
                    'SUPPORT_OUTSIDE_CSIP',
                    'REFERRAL_SUBMITTED',
                    'REFERRAL_PENDING',
                    'UNKNOWN'));

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
    when ((old.outcome_id, old.conclusion) is distinct from (new.outcome_id, new.conclusion))
execute function update_status_from_decision();

create or replace trigger decision_actions_delete_csip_status
    after delete
    on decision_and_actions
    for each row
execute function update_status_from_decision();

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

