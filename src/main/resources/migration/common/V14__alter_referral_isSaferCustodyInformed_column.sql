ALTER TABLE referral ALTER COLUMN safer_custody_team_informed TYPE varchar(12);

alter table referral
    add constraint safer_custody_team_informed_enum_check check
        (safer_custody_team_informed in ('YES', 'NO', 'DO_NOT_KNOW'));
