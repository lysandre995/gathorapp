#!/usr/bin/env bash

# Wrapper: delegate to Python generator for cross-platform behavior
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v python3 >/dev/null 2>&1; then
    PY=python3
elif command -v python >/dev/null 2>&1; then
    PY=python
else
    echo "[ERROR] Python not found in PATH"
    exit 1
fi

"$PY" "$SCRIPT_DIR/generate_pdf.py" "$@"
