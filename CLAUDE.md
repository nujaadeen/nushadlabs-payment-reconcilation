# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PaymentReconcilationApplicationTests

# Run a single test method
./mvnw test -Dtest=ClassName#methodName
```

## Data generator (Python)

```bash
cd data-generator
pip install -r requirements.txt

# Generate default 100-row CSV
python generate_transactions.py

# Generate seed data for MongoDB init (2M rows, ~393 MB)
python generate_transactions.py --count 2000000 --output ../docker/mongo-init/transactions.csv --seed 42
```

`docker/mongo-init/*.csv` files are git-ignored — regenerate them locally with the script before starting Docker services.

## Architecture

**Runtime stack**: Java 21, Spring Boot 3.5.14, Maven

**Core dependencies and their roles**:
- `spring-boot-starter-batch` — primary processing engine; reconciliation runs as a partitioned Spring Batch `Job` → `Step` → `ItemReader` / `ItemProcessor` / `ItemWriter` pipeline
- `spring-boot-starter-data-jpa` — required by Spring Batch for its metadata tables; schema auto-initialized via `spring.batch.jdbc.initialize-schema: always`
- `spring-boot-starter-data-mongodb` — document persistence; `Transaction` documents stored in the `transactions` collection; queried during reconciliation to detect status discrepancies
- `spring-boot-starter-web` — REST API for triggering jobs and checking status
- `lombok` — used across model/entity classes; annotation processor wired in `pom.xml` for compile and test phases

**Data generator** (`data-generator/generate_transactions.py`): standalone Python script that produces synthetic transaction CSVs. Fields: `transaction_id`, `order_id`, `customer_id`, `amount`, `currency`, `status`, `created_at`, `merchant_id`, `payment_method`, `channel`.

**Package root**: `com.nushadlabs.payment_reconcilation`

**Note**: `PaymentReconcilationApplication` uses `scanBasePackages = {"com.nushadlabs.payment_reconcilation", "com.recon"}` — the `com.recon` package is reserved for future components.

## Batch pipeline (implemented)

The pipeline is a partitioned job that splits a CSV by line ranges and processes each partition in parallel worker steps.

### Classes

| Class | Role |
|---|---|
| `batch/ReconJobConfig` | Wires `Job`, `masterStep`, `workerStep`, partitioner, thread-pool executor, async `JobLauncher`, and `ReconJobExecutionListener` |
| `batch/CsvLineRangePartitioner` | Counts CSV lines, divides into equal ranges, puts `linesToSkip` + `maxItemCount` into each worker's `ExecutionContext` |
| `batch/ReconItemReader` | `@StepScope` `FlatFileItemReader<CsvTransaction>` — reads the CSV slice assigned to its partition using `filePath` (job param) + `linesToSkip`/`maxItemCount` (step context) |
| `batch/ReconItemProcessor` | Looks up each CSV row in MongoDB by `transactionId`; emits a `ReconPayload` only when CSV status is `COMPLETED` but the stored status is not — i.e. discrepancy rows only |
| `batch/ReconItemWriter` | Logs each `ReconPayload` (currently log-only; no persistence) |
| `batch/ReconJobExecutionListener` | `JobExecutionListener` that logs a structured summary before and after every job run — see below |

### Key design points

- `filePath` is a **job parameter** — both `CsvLineRangePartitioner` and `ReconItemReader` resolve it via `#{jobParameters['filePath']}` at step scope.
- Thread count is controlled by `recon.thread-pool-size` (default `4`). It sets `corePoolSize`, `maxPoolSize`, and `gridSize` of the partition handler simultaneously.
- Chunk size is 500 rows per commit.
- The `asyncJobLauncher` bean uses `SimpleAsyncTaskExecutor` so `POST /api/v1/recon/start` returns immediately with `202 Accepted` while the job runs in background.
- Spring Batch auto-run is **disabled** (`spring.batch.job.enabled: false`); jobs are only triggered via the REST endpoint.

### Job execution listener (`ReconJobExecutionListener`)

Registered on the `reconJob` bean via `.listener()`. Emits `[RECON]`-prefixed log lines surrounded by `=`-separator lines so the summary is easy to `grep` in long log files.

**`beforeJob`** — logged at job start:
- Job ID, file path (from job parameters), start timestamp, configured thread-pool size

**`afterJob`** — logged when the job finishes (COMPLETED or FAILED):
- Final status, exit code, total elapsed time formatted as `Xm Xs Xms`
- Per-step table (sorted by start time) covering every partition worker: step name, read count, write count, skip count, step duration
- On `FAILED`: exit description (first line) + exception class and message for every entry in `getAllFailureExceptions()` (SLF4J trailing-Throwable prints the full stack trace)

## REST API

**Base path**: `/api/v1/recon`

### `POST /api/v1/recon/start`

Triggers a new reconciliation job against a CSV file already present on the server filesystem.

**Query parameter**: `filePath` — absolute path to the CSV file on the server (e.g. `/data/transactions.csv`)

**Response** `202 Accepted`:
```json
{ "jobInstanceId": 1, "status": "STARTED" }
```

### `GET /api/v1/recon/status/{jobInstanceId}`

Returns the latest execution state for a job instance.

**Response** `200 OK`:
```json
{
  "jobInstanceId": 1,
  "jobExecutionId": 1,
  "status": "COMPLETED",
  "exitCode": "COMPLETED",
  "startTime": "2026-05-26T10:00:00",
  "endTime": "2026-05-26T10:01:30"
}
```

Returns `404` if the instance ID is unknown.

## Models

| Class | Type | Description |
|---|---|---|
| `model/CsvTransaction` | `record` | Parsed CSV row — 10 fields matching the CSV schema |
| `model/ReconPayload` | `record` | Output of the processor — fields: `transactionId`, `orderId`, `amount`, `currency`, `originalStatus`, `source` (`"recon"`), `reconTimestamp` |
| `model/Transaction` | `@Document("transactions")` | MongoDB document with field-mapped columns (`transaction_id`, `order_id`, etc.) |

## Repository

`repository/TransactionRepository` — `MongoRepository<Transaction, String>` with one custom query: `findByTransactionId(String)`.

## Configuration

Primary config is **`application.yml`**. `application.properties` sets only `spring.application.name` and is effectively superseded.

| Property | Default | Env var override |
|---|---|---|
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/recon_db` | `SPRING_DATA_MONGODB_URI` |
| `spring.data.mongodb.database` | `recon_db` | `SPRING_DATA_MONGODB_DATABASE` |
| `spring.batch.job.enabled` | `false` | — |
| `spring.batch.jdbc.initialize-schema` | `always` | — |
| `recon.thread-pool-size` | `4` | `RECON_THREAD_POOL_SIZE` |
| Management endpoints exposed | `health,info,metrics` | — |

Before running outside Docker you need a reachable MongoDB instance and a datasource for JPA (Spring Batch metadata). For local dev, add an H2 datasource or point at a local PostgreSQL.

## Docker

```bash
# First-time: generate the seed CSV (git-ignored, ~393 MB)
cd data-generator && python generate_transactions.py \
  --count 2000000 --output ../docker/mongo-init/transactions.csv --seed 42
cd ..

# Start all services (MongoDB + recon-app)
docker compose up --build

# Override thread pool or other env vars without editing docker-compose.yml
cp docker-compose.override.yml.example docker-compose.override.yml
# edit docker-compose.override.yml, then:
docker compose up --build
```

`mongo_data` is a named volume — destroy it with `docker compose down -v` to re-run init scripts.