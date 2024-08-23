with ref_data as
         (select rd.reference_data_id, row_number() over (partition by domain order by description) as row_num
          from reference_data rd
          where domain in (select distinct domain from reference_data))
update reference_data rd
set list_sequence = (select row_num from ref_data where ref_data.reference_data_id = rd.reference_data_id);