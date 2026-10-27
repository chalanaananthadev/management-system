#!/bin/sh
# Creates the applebay database, tables and sample rows on the pgAdmin server.
# Usage: sh database/create-database.sh

set -e

export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5433}"
export PGUSER="${PGUSER:-postgres}"
export PGPASSWORD="${PGPASSWORD:-1234}"

PSQL="${PSQL:-psql}"

exists=$("$PSQL" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'applebay'")
if [ "$exists" != "1" ]; then
  "$PSQL" -d postgres -c "CREATE DATABASE applebay OWNER postgres"
fi

DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
"$PSQL" -d applebay -v ON_ERROR_STOP=1 -f "$DIR/schema.sql"
"$PSQL" -d applebay -v ON_ERROR_STOP=1 -f "$DIR/sample-data.sql"

echo "Database applebay is ready on ${PGHOST}:${PGPORT}."
