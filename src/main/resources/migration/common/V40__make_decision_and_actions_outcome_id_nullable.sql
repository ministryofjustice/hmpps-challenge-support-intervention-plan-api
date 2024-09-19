alter table decision_and_actions
    alter column outcome_id drop not null,
    alter column signed_off_by_role_id drop not null;