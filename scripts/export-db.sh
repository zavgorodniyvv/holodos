#!/bin/bash
# Exports local PostgreSQL database to a dump file.
# Usage: ./scripts/export-db.sh [output-file]

set -e

OUTPUT=${1:-holodos-backup-$(date +%Y%m%d_%H%M%S).dump}

echo "Dumping local database to: $OUTPUT"
pg_dump -h localhost -p 5432 -U holodos -d holodos -F c -f "$OUTPUT"
echo "Done. File size: $(du -sh "$OUTPUT" | cut -f1)"
