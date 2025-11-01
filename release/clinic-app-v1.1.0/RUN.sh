#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$(ls "${SCRIPT_DIR}"/clinic-app-*.jar | head -n 1)"
if [ ! -f "${JAR_PATH}" ]; then
    echo "Jar file not found. Did you run the release packaging script?" >&2
    exit 1
fi
exec java -jar "${JAR_PATH}"
