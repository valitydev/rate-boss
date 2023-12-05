CREATE SCHEMA IF NOT EXISTS rb;


CREATE TABLE rb.ex_rate
(
    id                                 BIGSERIAL PRIMARY KEY,
    created_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    source_currency_symbolic_code      CHARACTER VARYING NOT NULL,
    source_currency_exponent           SMALLINT          NOT NULL,
    destination_currency_symbolic_code CHARACTER VARYING NOT NULL,
    destination_currency_exponent      SMALLINT          NOT NULL,
    rational_p                         BIGINT            NOT NULL,
    rational_q                         BIGINT            NOT NULL,
    rate_timestamp                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    UNIQUE (source_currency_symbolic_code, destination_currency_symbolic_code)
);

CREATE INDEX source_currency_sc_destination_currency_sc_idx ON rb.ex_rate (source_currency_symbolic_code,
                                                                           destination_currency_symbolic_code);
