-- The Offender SAR team's decisions from the data review (IR-2067), returned 27 October 2026.
--
-- They agreed the proposal in V69 for every column except one, which moves from [SAR: Y] to [SAR: N]:
--
--   * referral.referer_area_of_work_id - the area of work of the member of staff who made the referral is
--     information about that member of staff, not the prisoner.
--
-- They also answered question 5 of the review: they want earlier versions of a CSIP in the report, not only the
-- current one. That is not reflected here. The *_audit columns stay [SAR: N] until it is agreed which versions
-- are needed and how they are shown.
--
-- Comments only - no schema or data change. The rest of each comment, including its example, is left as it is.

DO $$
DECLARE
  rec      record;
  existing text;
BEGIN
  FOR rec IN
    SELECT *
      FROM (
        VALUES
          ('referral', 'referer_area_of_work_id')
      ) AS t(table_name, column_name)
  LOOP
    SELECT col_description(a.attrelid, a.attnum)
      INTO existing
      FROM pg_attribute a
     WHERE a.attrelid = format('%I', rec.table_name)::regclass
       AND a.attname = rec.column_name
       AND a.attnum > 0
       AND NOT a.attisdropped;

    IF existing IS NULL OR position('[SAR: Y]' IN existing) = 0 THEN
      RAISE EXCEPTION 'Comment on %.% is not tagged [SAR: Y]', rec.table_name, rec.column_name;
    END IF;

    EXECUTE format(
      'COMMENT ON COLUMN %I.%I IS %L',
      rec.table_name,
      rec.column_name,
      replace(existing, '[SAR: Y]', '[SAR: N]')
    );
  END LOOP;
END $$;
