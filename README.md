# rate-boss

Rate-boss periodically pulls exchange rates from external sources, stores them in the database, and optionally publishes rate events to Kafka. It also exposes API endpoints to read the latest or historical rates and to convert amounts by timestamp.

Supported sources today: Fixer, CBR, NBKZ.

## Service flow (actual)

```mermaid
flowchart TD
  subgraph Quartz Scheduler
    M[ExchangeGrabberMasterJob<br/>per source] -->|per currency| J[ExchangeGrabberJob]
  end

  J -->|HTTP fetch| S[ExchangeRateSource<br/>Fixer / CBR / NBKZ]
  S -->|rates + timestamp| D[ExchangeDaoService]
  D -->|save batch| DB[(PostgreSQL<br/>rb.ex_rate)]
  J -->|optional publish| E[ExchangeEventService]
  E --> K[(Kafka topic)]

  API[ExchangeRateServiceSrv] -->|getExchangeRateData / getConvertedAmount| DB
```

## Quartz execution

For each configured source:
- `ExchangeGrabberMasterJob` reads the list of base currencies from `rates.*Job.currencies`.
- It spawns `ExchangeGrabberJob` per currency.
- Each `ExchangeGrabberJob` calls the respective `ExchangeRateSource`, saves the rates, and (for some sources) emits Kafka events.
