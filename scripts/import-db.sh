#!/bin/bash
# Imports a database dump into the running Docker Compose postgres container.
# Usage: ./scripts/import-db.sh <dump-file>

set -e

DUMP_FILE=${1:?Usage: $0 <dump-file>}

if [ ! -f "$DUMP_FILE" ]; then
  echo "Error: file not found: $DUMP_FILE"
  exit 1
fi

CONTAINER=$(docker compose ps -q postgres)
if [ -z "$CONTAINER" ]; then
  echo "Error: postgres container is not running. Start it with: docker compose up -d postgres"
  exit 1
fi

echo "Copying dump to container..."
docker cp "$DUMP_FILE" "$CONTAINER:/tmp/holodos.dump"

echo "Restoring database..."
docker exec "$CONTAINER" pg_restore -U holodos -d holodos -c --if-exists /tmp/holodos.dump

echo "Done."
