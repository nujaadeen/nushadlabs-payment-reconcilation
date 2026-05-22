# docker/mongo-init

Files placed here are mounted read-only into the MongoDB container at
`/docker-entrypoint-initdb.d/` and executed once on first startup (when
`mongo_data` volume is empty).

## Seed data (transactions.csv)

The `transactions.csv` file is **git-ignored** due to its size (~393 MB for 2M rows).
Generate it before running `docker compose up`:

```bash
cd data-generator
pip install -r requirements.txt
python generate_transactions.py \
  --count 2000000 \
  --output ../docker/mongo-init/transactions.csv \
  --seed 42
```

To use a different dataset, swap in any CSV with the same schema and rename it
`transactions.csv`, or update the init script that references it.

## Execution order

MongoDB runs init files in **alphabetical order**. Name scripts with a numeric
prefix (`01-`, `02-`, …) if order matters.