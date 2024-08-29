update reference_data
set list_sequence = 99
where domain = 'INTERVIEWEE_ROLE'
  and code = 'OTHER';

update reference_data
set list_sequence = 10
where domain = 'INTERVIEWEE_ROLE'
  and code = 'PERP';

update reference_data
set list_sequence = 20
where domain = 'INTERVIEWEE_ROLE'
  and code = 'VICTIM';

update reference_data
set list_sequence = 30
where domain = 'INTERVIEWEE_ROLE'
  and code = 'WITNESS';

update reference_data
set deactivated_at = '2024-08-29 00:00:00', deactivated_by = 'MOVE_AND_IMPROVE_TEAM'
where domain = 'SCREENING_OUTCOME_TYPE'
  and code = 'ACC';