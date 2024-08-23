insert into reference_data(domain, code, description, list_sequence, created_at, created_by)
values ('DECISION_SIGNER_ROLE', 'OTHER', 'Another authorised person', 999, current_date, 'SYS');

update decision_and_actions
set signed_off_by_role_id = (select reference_data_id
                             from reference_data
                             where domain = 'DECISION_SIGNER_ROLE'
                               and code = 'OTHER')
where signed_off_by_role_id is null;

alter table decision_and_actions
    alter column signed_off_by_role_id set not null;

