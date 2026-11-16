#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3.10+ is required. Install Python and retry."
  exit 1
fi
python3 - <<'PY'
import sys
if sys.version_info < (3, 10):
    raise SystemExit("Python 3.10+ is required.")
print("Python", sys.version.split()[0])
PY
if [ ! -d .venv ]; then python3 -m venv .venv; fi
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r backend/requirements.txt
if [ ! -f .env ]; then cp .env.example .env; echo "Created .env. Add at least one provider API key before AI chat."; fi
python -m compileall -q backend
python -m pytest -q
echo "Setup complete. Start with: ./scripts/run.sh"
