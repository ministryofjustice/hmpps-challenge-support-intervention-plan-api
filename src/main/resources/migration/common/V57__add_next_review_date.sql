alter table plan
    add column if not exists next_review_date date,
    add column if not exists closed_date      date;

-- index for review date required for determining late reviews
create index if not exists idx_plan_next_review_date on plan (next_review_date);

comment on column plan.next_review_date is 'Next review date is calculated and stored in this field for performance reasons. This will be the first case review date when no reviews exist or the next review date of the most recent review when a review exists.';
comment on column plan.closed_date is 'Closed date is calculated and stored in this field for performance reasons. This will be the closed date from the review that closes the CSIP.';

update plan
set next_review_date = case
                           when exists(select 1 from review where review.plan_id = plan.plan_id)
                               then (select next_review_date
                                     from review
                                     where review.plan_id = plan.plan_id
                                     order by review.review_sequence desc
                                     limit 1)
                           else plan.first_case_review_date end
where plan.next_review_date is null;

update plan
set closed_date = closed_date = (select csip_closed_date
                                 from review
                                 where review.plan_id = plan.plan_id
                                 order by review_sequence desc
                                 limit 1)
from csip_record csip
         join reference_data status on status.reference_data_id = csip.status_id
where plan.plan_id = csip.record_id
  and plan.closed_date is null
  and status.code = 'CSIP_CLOSED';

drop view csip_summary;
create view csip_summary
as
select person.prison_number,
       person.first_name,
       person.last_name,
       person.restricted_patient,
       person.prison_code,
       person.cell_location,
       person.supporting_prison_code,
       ref.referral_date,
       p.next_review_date as next_review_date,
       p.case_manager,
       csip.record_id     as id,
       case
           when st.code = 'CSIP_CLOSED' then p.closed_date
           end            as closed_date,
       case
           when st.code = 'CSIP_OPEN' then 1
           when st.code = 'CSIP_CLOSED' then 3
           when st.code in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
           else 2
           end            as priority,
       st.code            as status_code,
       st.description     as status_description
from csip_record csip
         join person_summary person on person.prison_number = csip.prison_number
         join referral ref on ref.referral_id = csip.record_id
         left join plan p on p.plan_id = csip.record_id
         join reference_data st on st.reference_data_id = csip.status_id;