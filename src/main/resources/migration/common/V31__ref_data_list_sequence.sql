with ref_data as
         (select rd.reference_data_id, row_number() over (partition by domain order by description) as row_num
          from reference_data rd
          where domain in (select distinct domain from reference_data))
update reference_data rd
set list_sequence = (select (row_num * 10) from ref_data where ref_data.reference_data_id = rd.reference_data_id);

update reference_data
    set list_sequence = 10
    where domain = 'INCIDENT_INVOLVEMENT'
      and code = 'PER';

update reference_data
    set list_sequence = 20
    where domain = 'INCIDENT_INVOLVEMENT'
      and code = 'VIC';

update reference_data
    set list_sequence = 30
    where domain = 'INCIDENT_INVOLVEMENT'
      and code = 'WIT';

update reference_data
    set list_sequence = 40
    where domain = 'INCIDENT_INVOLVEMENT'
      and code = 'OTH';

update reference_data
    set list_sequence = 10
    where domain = 'OUTCOME_TYPE'
      and code = 'CUR';

update reference_data
    set list_sequence = 20
    where domain = 'OUTCOME_TYPE'
      and code = 'OPE';

update reference_data
    set list_sequence = 30
    where domain = 'OUTCOME_TYPE'
      and code = 'WIN';

update reference_data
    set list_sequence = 40
    where domain = 'OUTCOME_TYPE'
      and code = 'ACC';

update reference_data
    set list_sequence = 50
    where domain = 'OUTCOME_TYPE'
      and code = 'NFA';

update reference_data
    set list_sequence = 10
    where domain = 'DECISION_SIGNER_ROLE'
      and code = 'CUSTMAN';

update reference_data
    set list_sequence = 20
    where domain = 'DECISION_SIGNER_ROLE'
      and code = 'THEHOFF';

update reference_data
    set list_sequence = 30
    where domain = 'DECISION_SIGNER_ROLE'
      and code = 'OTHER';

alter table reference_data
    add constraint unq_reference_data_domain_list_sequence unique (domain, list_sequence);
