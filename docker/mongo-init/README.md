# docker/mongo-init

Files in this directory are mounted read-only into the MongoDB container at
`/docker-entrypoint-initdb.d/` and executed **once**, in alphabetical order,
only when the `mongo_data` volume is empty (first start).

| File | Purpose |
|---|---|
| `01_import_transactions.sh` | Imports `transactions.csv` into `recon_db.transactions` via `mongoimport` |
| `02_create_indexes.js` | Creates indexes on the collection (runs as fallback even if import is skipped) |
| `transactions.csv` | Seed data — **git-ignored**, must be generated locally (see below) |

---

## CSV seed data

`transactions.csv` is git-ignored (~393 MB for 2 M rows). Generate it before
running `docker compose up`:

```bash
cd data-generator
pip install -r requirements.txt
python generate_transactions.py \
  --count 2000000 \
  --output ../docker/mongo-init/transactions.csv \
  --seed 42
```

### Replacing with a different file

Place any CSV with the schema below at `docker/mongo-init/transactions.csv`
before starting the stack. The filename must match exactly.

### Expected CSV column format

```
transaction_id,order_id,customer_id,amount,currency,status,created_at,merchant_id,payment_method,channel
```

| Column | Type | Example |
|---|---|---|
| `transaction_id` | UUID string | `bdd640fb-0667-...` |
| `order_id` | UUID string | `23b8c1e9-3924-...` |
| `customer_id` | UUID string | `bd9c66b3-ad3c-...` |
| `amount` | Decimal (2 dp) | `8921.89` |
| `currency` | `USD` / `EUR` / `GBP` / `AED` | `USD` |
| `status` | `COMPLETED` / `PENDING` / `FAILED` | `COMPLETED` |
| `created_at` | ISO 8601 UTC | `2026-05-19T19:04:38Z` |
| `merchant_id` | UUID string | `815ef6d1-3b8f-...` |
| `payment_method` | `CARD` / `BANK_TRANSFER` / `WALLET` | `CARD` |
| `channel` | `ONLINE` / `POS` / `MOBILE` | `ONLINE` |

---

## Init scripts only run once

MongoDB executes init scripts **only when the data volume is empty**. If the
`mongo_data` volume already exists from a previous run, the scripts are skipped
entirely — even if you add new files or change existing ones.

### Resetting the database

To force a clean re-initialisation:

```bash
# Tear down containers AND delete the named volume
docker compose down -v

# Regenerate seed CSV if needed (see above), then start fresh
docker compose up
```

> `down -v` permanently deletes all data in the `mongo_data` volume.

---

## Indexes created

`02_create_indexes.js` ensures the following indexes exist on `recon_db.transactions`:

| Index name | Field | Options |
|---|---|---|
| `idx_transaction_id_unique` | `transaction_id` | unique |
| `idx_status` | `status` | — |
| `idx_created_at` | `created_at` | — |
| `idx_customer_id` | `customer_id` | — |