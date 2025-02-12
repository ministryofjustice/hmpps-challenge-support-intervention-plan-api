alter table investigation
    add column if not exists recorded_by varchar(255);

alter table investigation_audit
    add column if not exists recorded_by          varchar(255),
    add column if not exists recorded_by_modified boolean;