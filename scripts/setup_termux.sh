#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${PREFIX:-}" ] || [ ! -d "$PREFIX" ] || [ -z "${TERMUX_VERSION:-}" ]; then
  echo "Run this script inside the Termux app from F-Droid or the official GitHub release."
  exit 1
fi

pkg update -y
pkg install -y python git
python - <<'PY'
import sys
if sys.version_info < (3, 10):
    raise SystemExit("Python 3.10+ is required. Update Termux packages and retry.")
print("Python", sys.version.split()[0])
PY

if [ ! -d .venv ]; then python -m venv .venv; fi
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r backend/requirements.txt
if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env. Add at least one provider API key before AI chat."
fi
python -m compileall -q backend
python -m pytest -q
echo
echo "Setup complete. Start Zaythar AI with: bash scripts/run_termux.sh"
