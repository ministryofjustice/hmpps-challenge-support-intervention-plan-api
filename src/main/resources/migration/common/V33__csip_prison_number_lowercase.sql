create index idx_csip_record_prison_number_lower on csip_record (lower(prison_number));
create index idx_csip_record_created_at on csip_record(created_at)