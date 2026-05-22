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

# Large file generation
python generate_transactions.py --count 1000000 --output ../data/txns.csv --seed 99
```

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

## Configuration notes

`application.properties` currently only sets `spring.application.name`. Before the app can start successfully, it will need datasource configuration for JPA (e.g. H2 for local dev or PostgreSQL) and a MongoDB URI, otherwise Spring Boot auto-configuration will fail context load.