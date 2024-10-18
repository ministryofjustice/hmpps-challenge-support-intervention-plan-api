create index idx_person_summary_prison_code_lower on person_summary (lower(prison_code));
create index idx_person_summary_first_name_lower on person_summary (lower(first_name));
create index idx_person_summary_last_name_lower on person_summary (lower(last_name));

create index idx_csip_record_status on csip_record (status);