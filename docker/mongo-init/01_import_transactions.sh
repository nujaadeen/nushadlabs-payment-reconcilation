#!/bin/bash
set -euo pipefail

CSV="/docker-entrypoint-initdb.d/transactions.csv"
DB="recon_db"
COLLECTION="transactions"

if [ ! -f "$CSV" ]; then
  echo "[init] WARNING: $CSV not found — skipping import."
  echo "[init] Generate it with: python data-generator/generate_transactions.py --count 10 --output docker/mongo-init/transactions.csv --seed 42"
  exit 0
fi

echo "[init] Importing $CSV into $DB.$COLLECTION ..."

mongoimport \
  --host localhost \
  --db "$DB" \
  --collection "$COLLECTION" \
  --type csv \
  --headerline \
  --file "$CSV"

COUNT=$(mongosh --quiet --eval "db.getSiblingDB('$DB').$COLLECTION.countDocuments({})")
echo "[init] Import complete — $COUNT documents in $DB.$COLLECTION"