// Fallback index creation — runs after 01_import_transactions.sh.
// mongoimport creates the collection; this script ensures indexes exist
// regardless of whether the import step ran.

const db = db.getSiblingDB("recon_db");
const col = db.getCollection("transactions");

col.createIndex({ transaction_id: 1 }, { unique: true, name: "idx_transaction_id_unique" });
col.createIndex({ status: 1 },         { name: "idx_status" });
col.createIndex({ created_at: 1 },     { name: "idx_created_at" });
col.createIndex({ customer_id: 1 },    { name: "idx_customer_id" });

print("[init] Indexes on recon_db.transactions:");
col.getIndexes().forEach(idx => print("  " + idx.name));