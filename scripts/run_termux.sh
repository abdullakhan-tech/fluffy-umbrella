#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -x .venv/bin/python ]; then
  echo "Run bash scripts/setup_termux.sh first."
  exit 1
fi
echo "Zaythar AI will be available on this phone at http://127.0.0.1:8000"
echo "Keep Termux open while using the app. Press Ctrl+C to stop."
exec .venv/bin/python -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
