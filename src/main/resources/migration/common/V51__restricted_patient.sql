alter table person_summary
    add column restricted_patient     boolean not null default false,
    add column supporting_prison_code varchar(16);

alter table person_summary_audit
    add column restricted_patient              boolean     not null default false,
    add column supporting_prison_code          varchar(16),
    add column restricted_patient_modified     boolean     not null default false,
    add column supporting_prison_code_modified varchar(16) not null default false;

drop view csip_summary;
create view csip_summary as
with latest_review as (select plan_id,
                              next_review_date,
                              csip_closed_date,
                              row_number() over (partition by plan_id order by review_sequence desc) seq
                       from review rev)
select person.prison_number,
       person.first_name,
       person.last_name,
       person.restricted_patient,
       person.prison_code,
       person.cell_location,
       person.supporting_prison_code,
       ref.referral_date,
       coalesce(rev.next_review_date, p.first_case_review_date)        as next_review_date,
       p.case_manager,
       csip.record_id                                                  as id,
       case when st.code = 'CSIP_CLOSED' then rev.csip_closed_date end as closed_date,
       case
           when st.code = 'CSIP_OPEN' then 1
           when st.code = 'CSIP_CLOSED' then 3
           when st.code in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
           else 2
           end                                                         as priority,
       st.code                                                         as status_code,
       st.description                                                  as status_description
from csip_record csip
         join person_summary person on person.prison_number = csip.prison_number
         join referral ref on ref.referral_id = csip.record_id
         join reference_data st on st.reference_data_id = csip.status_id
         left join plan p on p.plan_id = csip.record_id
         left join latest_review rev on rev.plan_id = p.plan_id and rev.seq = 1;