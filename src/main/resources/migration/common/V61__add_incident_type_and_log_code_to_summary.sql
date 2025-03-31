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
       inc.description    as incident_type,
       p.next_review_date as next_review_date,
       p.case_manager,
       csip.record_id     as id,
       csip.log_code      as log_code,
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
         join reference_data inc on inc.reference_data_id = ref.incident_type_id
         left join plan p on p.plan_id = csip.record_id
         join reference_data st on st.reference_data_id = csip.status_id;