alter table safer_custody_screening_outcome
    alter column reason_for_decision drop not null;

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

create or replace function delete_csip_records(csip_ids uuid[], delete_audit boolean default true) returns void as
$$
declare
    attendee_ids  uuid[];
    review_ids    uuid[];
    need_ids      uuid[];
    interview_ids uuid[];
    factor_ids    uuid[];
begin

    select array_agg(a.attendee_id)
    into attendee_ids
    from attendee a
             join review rev on rev.review_id = a.review_id
    where rev.plan_id = any (csip_ids);

    delete from attendee where attendee_id = any (attendee_ids);

    if (delete_audit) then
        delete from attendee_audit where attendee_id = any (attendee_ids);
    end if;

    select array_agg(rev.review_id)
    into review_ids
    from review rev
    where rev.plan_id = any (csip_ids);

    delete from review where review_id = any (review_ids);

    if (delete_audit) then
        delete from review_audit where review_id = any (review_ids);
    end if;

    select array_agg(n.identified_need_id)
    into need_ids
    from identified_need n
    where n.plan_id = any (csip_ids);

    delete from identified_need where identified_need_id = any (need_ids);

    if (delete_audit) then
        delete from identified_need_audit where identified_need_id = any (need_ids);
    end if;

    select array_agg(rev.interview_id)
    into interview_ids
    from interview rev
    where rev.investigation_id = any (csip_ids);

    delete from interview where interview_id = any (interview_ids);

    if (delete_audit) then
        delete from interview_audit where interview_id = any (interview_ids);
    end if;

    select array_agg(rev.contributory_factor_id)
    into factor_ids
    from contributory_factor rev
    where rev.referral_id = any (csip_ids);

    delete from contributory_factor where contributory_factor_id = any (factor_ids);

    if (delete_audit) then
        delete from contributory_factor_audit where contributory_factor_id = any (factor_ids);
    end if;

    delete from plan where plan_id = any (csip_ids);
    if (delete_audit) then
        delete from plan_audit where plan_audit.plan_id = any (csip_ids);
    end if;

    delete from decision_and_actions where decision_and_actions_id = any (csip_ids);
    if (delete_audit) then
        delete
        from decision_and_actions_audit
        where decision_and_actions_audit.decision_and_actions_id = any (csip_ids);
    end if;

    delete from investigation where investigation_id = any (csip_ids);
    if (delete_audit) then
        delete from investigation_audit where investigation_audit.investigation_id = any (csip_ids);
    end if;

    delete from safer_custody_screening_outcome where safer_custody_screening_outcome_id = any (csip_ids);
    if (delete_audit) then
        delete
        from safer_custody_screening_outcome_audit
        where safer_custody_screening_outcome_audit.safer_custody_screening_outcome_id = any (csip_ids);
    end if;

    delete from referral where referral_id = any (csip_ids);
    if (delete_audit) then
        delete from referral_audit where referral_audit.referral_id = any (csip_ids);
    end if;

    delete from csip_record where record_id = any (csip_ids);
    if (delete_audit) then
        delete from csip_record_audit where csip_record_audit.record_id = any (csip_ids);
    end if;

    if (delete_audit) then
        perform remove_orphan_revisions();
    end if;
end;
$$ language plpgsql;

-- Example usage --
/*
    with ids as (select record_id from csip_record)
    select delete_csip_records((select array_agg(record_id) from ids));
*/