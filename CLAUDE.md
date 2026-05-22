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
- `spring-boot-starter-batch` — the primary processing engine; reconciliation logic will live in Spring Batch `Job` → `Step` → `ItemReader` / `ItemProcessor` / `ItemWriter` pipelines
- `spring-boot-starter-data-jpa` — relational persistence (datasource not yet configured in `application.properties`)
- `spring-boot-starter-data-mongodb` — document persistence (connection not yet configured); likely for raw transaction storage or audit records
- `spring-boot-starter-web` — REST API for triggering jobs or exposing reconciliation results
- `lombok` — used across model/entity classes; annotation processor is wired in `pom.xml` for both compile and test phases

**Data generator** (`data-generator/generate_transactions.py`): standalone Python script that produces synthetic transaction CSVs consumed by the batch jobs. Fields: `transaction_id`, `order_id`, `customer_id`, `amount`, `currency`, `status`, `created_at`, `merchant_id`, `payment_method`, `channel`.

**Package root**: `com.nushadlabs.payment_reconcilation`

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

## Configuration notes

`application.properties` currently only sets `spring.application.name`. Before the app can start successfully outside Docker, it will need a MongoDB URI and datasource configuration for JPA (e.g. H2 for local dev or PostgreSQL), otherwise Spring Boot auto-configuration will fail context load.