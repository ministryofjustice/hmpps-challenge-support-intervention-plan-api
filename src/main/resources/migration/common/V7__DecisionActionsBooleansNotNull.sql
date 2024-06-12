 update decisions_and_actions set action_open_csip_alert = false where action_open_csip_alert is null;
 update decisions_and_actions set action_non_associations_updated = false where action_non_associations_updated is null;
 update decisions_and_actions set action_observation_book = false where action_observation_book is null;
 update decisions_and_actions set action_unit_or_cell_move = false where action_unit_or_cell_move is null;
 update decisions_and_actions set action_csra_or_rsra_review = false where action_csra_or_rsra_review is null;
 update decisions_and_actions set action_service_referral = false where action_service_referral is null;
 update decisions_and_actions set action_sim_referral = false where action_sim_referral is null;

alter table decisions_and_actions alter column action_open_csip_alert set not null;
alter table decisions_and_actions alter column action_non_associations_updated set not null;
alter table decisions_and_actions alter column action_observation_book set not null;
alter table decisions_and_actions alter column action_unit_or_cell_move set not null;
alter table decisions_and_actions alter column action_csra_or_rsra_review set not null;
alter table decisions_and_actions alter column action_service_referral set not null;
alter table decisions_and_actions alter column action_sim_referral set not null;