drop view csip_summary;
create view csip_summary
as
with latest_review as (select distinct on (plan_id) plan_id,
                                                    review_id,
                                                    next_review_date,
                                                    csip_closed_date
                       from review
                       order by plan_id, review_sequence desc)
select person.prison_number,
       person.first_name,
       person.last_name,
       person.restricted_patient,
       person.prison_code,
       person.cell_location,
       person.supporting_prison_code,
       ref.referral_date,
       case
           when rev.review_id is not null then rev.next_review_date
           else p.first_case_review_date end as next_review_date,
       p.case_manager,
       csip.record_id                        as id,
       case
           when st.code = 'CSIP_CLOSED' then rev.csip_closed_date
           end                               as closed_date,
       case
           when st.code = 'CSIP_OPEN' then 1
           when st.code = 'CSIP_CLOSED' then 3
           when st.code in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
           else 2
           end                               as priority,
       st.code                               as status_code,
       st.description                        as status_description
from csip_record csip
         join person_summary person on person.prison_number = csip.prison_number
         join referral ref on ref.referral_id = csip.record_id
         left join plan p on p.plan_id = csip.record_id
         left join latest_review rev on rev.plan_id = p.plan_id
         join reference_data st on st.reference_data_id = csip.status_id;