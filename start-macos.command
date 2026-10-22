#!/bin/bash
set -eu
cd -- "$(dirname -- "$0")"
if ! command -v python3 >/dev/null 2>&1; then
  echo "Cần Python 3.10 trở lên: https://www.python.org/downloads/"
  read -r -p "Nhấn Enter để đóng…" _answer
  exit 1
fi
exec python3 server.py
