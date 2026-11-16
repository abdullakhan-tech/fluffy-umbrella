#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -d .venv ]; then echo "Run ./scripts/setup.sh first."; exit 1; fi
exec .venv/bin/python -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
