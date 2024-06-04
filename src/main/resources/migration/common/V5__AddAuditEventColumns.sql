alter table audit_event add column source varchar(12);
alter table audit_event add column reason varchar(12);
alter table audit_event add column active_case_load_id varchar(6);