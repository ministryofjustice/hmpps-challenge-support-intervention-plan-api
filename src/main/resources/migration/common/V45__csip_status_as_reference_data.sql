alter table reference_data
    alter column code type varchar(32);

alter table reference_data
    drop constraint reference_data_domain_enum_check,
    add constraint reference_data_domain_enum_check check
        (domain in ('AREA_OF_WORK', 'CONTRIBUTORY_FACTOR_TYPE', 'DECISION_OUTCOME_TYPE', 'DECISION_SIGNER_ROLE',
                    'INCIDENT_INVOLVEMENT', 'INCIDENT_LOCATION', 'INCIDENT_TYPE', 'INTERVIEWEE_ROLE',
                    'SCREENING_OUTCOME_TYPE', 'STATUS'));

with status_rd as (select object_id as code, column2 as description, row_number() over (order by column2) as seq
                   from (values ('ACCT_SUPPORT', 'Support through ACCT'),
                                ('AWAITING_DECISION', 'Awaiting decision'),
                                ('CSIP_CLOSED', 'CSIP closed'),
                                ('CSIP_OPEN', 'CSIP open'),
                                ('INVESTIGATION_PENDING', 'Investigation pending'),
                                ('NO_FURTHER_ACTION', 'No further action'),
                                ('PLAN_PENDING', 'Plan pending'),
                                ('REFERRAL_PENDING', 'Referral pending'),
                                ('REFERRAL_SUBMITTED', 'Referral submitted'),
                                ('SUPPORT_OUTSIDE_CSIP', 'Support outside of CSIP')) as t(object_id, column2))
insert
into reference_data(domain, code, description, list_sequence, created_at, created_by)
select 'STATUS', st.code, st.description, st.seq, current_date, 'SYS'
from status_rd st
where not exists(select 1 from reference_data where domain = 'status' and code = st.code);

alter table csip_record
    add column status_id bigint references reference_data (reference_data_id);

with csip_status as (select reference_data_id as id, code
                     from reference_data
                     where domain = 'STATUS')
update csip_record csip
set status_id = csip_status.id
from csip_status
where csip.status = csip_status.code;

alter table csip_record
    alter column status_id set not null;

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
       csip.record_id                                           as id,
       csip.created_at,
       case
           when st.code = 'CSIP_OPEN' then 1
           when st.code = 'CSIP_CLOSED' then 3
           when st.code in ('NO_FURTHER_ACTION', 'SUPPORT_OUTSIDE_CSIP') then 4
           else 2
           end                                                  as priority,
       st.code                                                  as status_code,
       st.description                                           as status_description
from csip_record csip
         join person_summary person on person.prison_number = csip.prison_number
         join referral ref on ref.referral_id = csip.record_id
         join reference_data st on st.reference_data_id = csip.status_id
         left join plan p on p.plan_id = csip.record_id
         left join latest_review rev on rev.plan_id = p.plan_id and rev.seq = 1;

create or replace function set_status_on_csip() returns trigger as
$$
begin
    select reference_data_id into new.status_id from reference_data where code = calculate_csip_status(new.record_id);
    return new;
end;
$$ language plpgsql;

create or replace function update_csip_status(csip_id uuid) returns void as
$$
begin
    update csip_record
    set status_id = (select reference_data_id from reference_data where code = calculate_csip_status(csip_id))
    where record_id = csip_id;
end;
$$ language plpgsql;

alter table csip_record
    drop column status;