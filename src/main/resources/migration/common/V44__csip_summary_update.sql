drop view csip_summary;

create view csip_summary as
with latest_review as (select plan_id,
                              next_review_date,
                              row_number() over (partition by plan_id order by review_sequence desc) seq
                       from review rev)
select person.prison_number,
       person.first_name,
       person.last_name,
       person.prison_code,
       person.cell_location,
       ref.referral_date,
       coalesce(rev.next_review_date, p.first_case_review_date) as next_review_date,
       p.case_manager,
       csip.status,
       csip.record_id                                           as id,
       csip.created_at,
       case
           when csip.status = 'CSIP_OPEN' then 1
           when csip.status = 'CSIP_CLOSED' then 3
           when csip.status in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
           else 2
           end                                                  as priority,
       case
           when csip.status = 'ACCT_SUPPORT' then 'support through acct'
           else lower(replace(csip.status, '_', ' '))
           end                                                  as status_description
from csip_record csip
         join person_summary person on person.prison_number = csip.prison_number
         join referral ref on ref.referral_id = csip.record_id
         left join plan p on p.plan_id = csip.record_id
         left join latest_review rev on rev.plan_id = p.plan_id and rev.seq = 1;