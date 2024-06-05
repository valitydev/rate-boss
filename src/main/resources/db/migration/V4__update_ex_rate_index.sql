ALTER TABLE rb.ex_rate DROP CONSTRAINT IF EXISTS ex_rate_source_currency_symbolic_code_destination_currency__key;
DROP INDEX IF EXISTS source_currency_sc_destination_currency_sc_idx;
CREATE INDEX source_source_cur_sc_destination_cur_sc_tstp_idx ON rb.ex_rate (source,
                                                                             source_currency_symbolic_code,
                                                                             destination_currency_symbolic_code,
                                                                             rate_timestamp);
