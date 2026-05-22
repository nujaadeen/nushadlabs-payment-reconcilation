import argparse
import csv
import random
import uuid
from datetime import datetime, timedelta, timezone

from tqdm import tqdm

CURRENCIES = ["USD", "EUR", "GBP", "AED"]
CURRENCY_WEIGHTS = [0.50, 0.25, 0.15, 0.10]

STATUSES = ["COMPLETED", "PENDING", "FAILED"]
STATUS_WEIGHTS = [0.70, 0.20, 0.10]

PAYMENT_METHODS = ["CARD", "BANK_TRANSFER", "WALLET"]
CHANNELS = ["ONLINE", "POS", "MOBILE"]

FIELDNAMES = [
    "transaction_id",
    "order_id",
    "customer_id",
    "amount",
    "currency",
    "status",
    "created_at",
    "merchant_id",
    "payment_method",
    "channel",
]

CHUNK_SIZE = 10_000


def random_amount(rng: random.Random) -> str:
    return f"{rng.uniform(1.00, 9999.99):.2f}"


def random_timestamp(rng: random.Random, now: datetime) -> str:
    delta_seconds = rng.randint(0, 90 * 24 * 3600)
    ts = now - timedelta(seconds=delta_seconds)
    return ts.strftime("%Y-%m-%dT%H:%M:%SZ")


def generate_row(rng: random.Random, now: datetime) -> dict:
    return {
        "transaction_id": str(uuid.UUID(int=rng.getrandbits(128))),
        "order_id": str(uuid.UUID(int=rng.getrandbits(128))),
        "customer_id": str(uuid.UUID(int=rng.getrandbits(128))),
        "amount": random_amount(rng),
        "currency": rng.choices(CURRENCIES, weights=CURRENCY_WEIGHTS)[0],
        "status": rng.choices(STATUSES, weights=STATUS_WEIGHTS)[0],
        "created_at": random_timestamp(rng, now),
        "merchant_id": str(uuid.UUID(int=rng.getrandbits(128))),
        "payment_method": rng.choice(PAYMENT_METHODS),
        "channel": rng.choice(CHANNELS),
    }


def write_transactions(count: int, output: str, seed: int) -> None:
    rng = random.Random(seed)
    now = datetime.now(tz=timezone.utc)

    status_counts: dict[str, int] = {s: 0 for s in STATUSES}
    currency_counts: dict[str, int] = {c: 0 for c in CURRENCIES}

    with open(output, "w", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=FIELDNAMES)
        writer.writeheader()

        with tqdm(total=count, unit="rows", desc="Generating") as bar:
            remaining = count
            while remaining > 0:
                chunk = min(remaining, CHUNK_SIZE)
                rows = [generate_row(rng, now) for _ in range(chunk)]
                writer.writerows(rows)
                for row in rows:
                    status_counts[row["status"]] += 1
                    currency_counts[row["currency"]] += 1
                remaining -= chunk
                bar.update(chunk)

    print(f"\nOutput : {output}")
    print(f"Total  : {count:,} rows")
    print("\nStatus breakdown:")
    for status, n in status_counts.items():
        print(f"  {status:<12} {n:>8,}  ({n / count * 100:.1f}%)")
    print("\nCurrency breakdown:")
    for currency, n in currency_counts.items():
        print(f"  {currency:<8} {n:>8,}  ({n / count * 100:.1f}%)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate synthetic transaction CSV data.")
    parser.add_argument("--count", type=int, default=100, help="Number of rows to generate (default: 100)")
    parser.add_argument("--output", default="transactions.csv", help="Output CSV file path (default: transactions.csv)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility (default: 42)")
    args = parser.parse_args()

    if args.count < 1:
        parser.error("--count must be at least 1")

    write_transactions(args.count, args.output, args.seed)


if __name__ == "__main__":
    main()