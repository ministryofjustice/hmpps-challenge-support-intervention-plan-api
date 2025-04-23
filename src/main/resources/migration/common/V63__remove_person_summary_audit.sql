drop table if exists person_summary_audit;
delete from audit_revision where affected_components = '{}';