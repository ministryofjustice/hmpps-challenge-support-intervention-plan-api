create or replace function remove_orphan_revisions() returns void as
$$
begin
    delete
    from audit_revision
    where id not in (select rev_id
                     from attendee_audit
                     union all
                     select rev_id
                     from review_audit
                     union all
                     select rev_id
                     from identified_need_audit
                     union all
                     select plan_id
                     from plan_audit
                     union all
                     select rev_id
                     from interview_audit
                     union all
                     select rev_id
                     from contributory_factor_audit
                     union all
                     select rev_id
                     from decision_and_actions_audit
                     union all
                     select rev_id
                     from investigation_audit
                     union all
                     select rev_id
                     from safer_custody_screening_outcome_audit
                     union all
                     select rev_id
                     from referral_audit
                     union all
                     select rev_id
                     from csip_record_audit);
end;
$$ language plpgsql;