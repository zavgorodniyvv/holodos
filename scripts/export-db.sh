#!/bin/bash
# Exports local PostgreSQL database to a dump file.
# Usage: ./scripts/export-db.sh [output-file]

set -e

OUTPUT=${1:-holodos-backup-$(date +%Y%m%d_%H%M%S).dump}

echo "Dumping database to: $OUTPUT"
docker compose exec -T postgres pg_dump -U holodos -d holodos -F c > "$OUTPUT"
echo "Done. File size: $(du -sh "$OUTPUT" | cut -f1)"
