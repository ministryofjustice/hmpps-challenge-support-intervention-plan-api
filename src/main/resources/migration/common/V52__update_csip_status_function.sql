create or replace function calculate_csip_status(csip_id uuid) returns varchar as
$$
declare
    actioned_to_close     boolean;
    ref_complete          boolean;
    screening_outcome     varchar;
    decision_outcome      varchar;
    investigation_started boolean;
begin
    actioned_to_close = actioned_to_close(csip_id);
    if actioned_to_close then
        return 'CSIP_CLOSED';
    elsif exists(select 1 from plan where plan_id = csip_id) then
        return 'CSIP_OPEN';
    end if;

    select coalesce(r.referral_complete, false), sout.code, dout.code, inv.investigation_id is not null
    into ref_complete, screening_outcome, decision_outcome, investigation_started
    from referral r
             left join safer_custody_screening_outcome scso on scso.safer_custody_screening_outcome_id = r.referral_id
             left join reference_data sout on sout.reference_data_id = scso.outcome_id
             left join decision_and_actions da on da.decision_and_actions_id = r.referral_id
             left join reference_data dout on dout.reference_data_id = da.outcome_id
             left join investigation inv on inv.investigation_id = r.referral_id
    where r.referral_id = csip_id;

    return case
               when 'NFA' in (screening_outcome, decision_outcome) then 'NO_FURTHER_ACTION'
               when 'WIN' in (screening_outcome, decision_outcome) then 'SUPPORT_OUTSIDE_CSIP'
               when 'ACC' in (screening_outcome, decision_outcome) then 'ACCT_SUPPORT'
               when 'CUR' in (screening_outcome, decision_outcome) then 'PLAN_PENDING'
               when decision_outcome is null and investigation_started then 'AWAITING_DECISION'
               when screening_outcome = 'OPE' and decision_outcome is null then 'INVESTIGATION_PENDING'
               when ref_complete then 'REFERRAL_SUBMITTED'
               else 'REFERRAL_PENDING'
        end;

end;
$$ language plpgsql;