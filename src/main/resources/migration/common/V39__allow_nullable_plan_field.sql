alter table plan
    alter column case_manager drop not null,
    alter column reason_for_plan drop not null,
    alter column first_case_review_date drop not null;