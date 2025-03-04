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
               when exists(select 1 from plan where plan_id = csip_id) then 'CSIP_OPEN'
               when 'CUR' in (screening_outcome, decision_outcome) then 'PLAN_PENDING'
               when decision_outcome is null and investigation_started then 'AWAITING_DECISION'
               when screening_outcome = 'OPE' and decision_outcome is null then 'INVESTIGATION_PENDING'
               when ref_complete then 'REFERRAL_SUBMITTED'
               else 'REFERRAL_PENDING'
        end;

end;
$$ language plpgsql;

with current_open as (select csip.record_id as id,
                             soo.code       as soc,
                             dao.code       as doc
                      from csip_record csip
                               join reference_data status on status.reference_data_id = csip.status_id
                               join referral ref on ref.referral_id = csip.record_id
                               left join decision_and_actions da on da.decision_and_actions_id = csip.record_id
                               left join reference_data dao on dao.reference_data_id = da.outcome_id
                               left join safer_custody_screening_outcome so
                                         on so.safer_custody_screening_outcome_id = csip.record_id
                               left join reference_data soo on soo.reference_data_id = so.outcome_id
                      where status.code = 'CSIP_OPEN')
select calculate_csip_status(co.id)
from current_open co
where coalesce(co.doc, co.soc) <> 'CUR';