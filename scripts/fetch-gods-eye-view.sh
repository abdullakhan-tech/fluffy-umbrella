#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/.local/gods-eye-view"
REPO="https://github.com/bilawalsidhu/gods-eye-view.git"
PIN="ce671ce500a393be27e3cbb2a08799fbca9b6e28"

if ! command -v git >/dev/null 2>&1; then
  echo "Git is required." >&2
  exit 1
fi

mkdir -p "$ROOT/.local"
if [ -e "$DEST/.git" ]; then
  echo "Existing checkout found at $DEST"
  git -C "$DEST" fetch --depth 1 origin "$PIN"
else
  if [ -e "$DEST" ]; then
    echo "Refusing to overwrite non-git directory: $DEST" >&2
    exit 1
  fi
  git clone --filter=blob:none --no-checkout "$REPO" "$DEST"
  git -C "$DEST" fetch --depth 1 origin "$PIN"
fi

git -C "$DEST" checkout --detach "$PIN"
echo
echo "Checked out God's Eye View at $PIN"
echo "Before redistribution or commercial deployment, review:"
echo "  $DEST/LICENSE"
echo "  $DEST/DATA_SOURCES.md"
echo "  $DEST/public/models/README.md"
echo "This is a local companion checkout, not code copied into the Zaythar APK."
