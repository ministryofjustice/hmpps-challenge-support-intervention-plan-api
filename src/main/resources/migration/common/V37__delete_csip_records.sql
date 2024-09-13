create or replace function delete_csip_records(csip_ids uuid[]) returns void as
$$
begin
    with attendee_ids as (select a.attendee_id
                          from attendee a
                                   join review rev on rev.review_id = a.review_id
                          where rev.plan_id = any (csip_ids))
    delete
    from attendee
    where attendee_id in (select attendee_id from attendee_ids);

    with review_ids as (select review_id from review where plan_id = any (csip_ids))
    delete
    from review
    where review_id in (select review_id from review_ids);

    with need_ids as (select identified_need_id from identified_need where plan_id = any (csip_ids))
    delete
    from identified_need
    where identified_need.identified_need_id in (select identified_need_id from need_ids);

    with interview_ids as (select interview_id from interview where investigation_id = any (csip_ids))
    delete
    from interview
    where interview_id in (select interview_id from interview_ids);

    with factor_ids as (select contributory_factor_id from contributory_factor where referral_id = any (csip_ids))
    delete
    from contributory_factor
    where contributory_factor_id in (select contributory_factor_id from factor_ids);

    delete from plan where plan_id = any (csip_ids);
    delete from decision_and_actions where decision_and_actions_id = any (csip_ids);
    delete from investigation where investigation_id = any (csip_ids);
    delete from safer_custody_screening_outcome where safer_custody_screening_outcome_id = any (csip_ids);
    delete from referral where referral_id = any (csip_ids);
    delete from csip_record where record_id = any (csip_ids);

end;
$$ language plpgsql;

-- Example usage --
/*
    with ids as (select record_id from csip_record)
    select delete_csip_records((select array_agg(record_id) from ids));
*/