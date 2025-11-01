#!/usr/bin/env bash
set -euo pipefail

if [ "${1-}" = "" ]; then
    echo "Usage: $0 <version>" >&2
    exit 1
fi

VERSION="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_DIR="${PROJECT_ROOT}/out"
RELEASE_ROOT="${PROJECT_ROOT}/release"
PACKAGE_DIR="${RELEASE_ROOT}/clinic-app-${VERSION}"
MANIFEST_FILE="${RELEASE_ROOT}/MANIFEST.MF"
JAR_FILE="${PACKAGE_DIR}/clinic-app-${VERSION}.jar"
ZIP_FILE="${RELEASE_ROOT}/clinic-app-${VERSION}.zip"

rm -rf "${OUT_DIR}" "${RELEASE_ROOT}"
mkdir -p "${OUT_DIR}" "${PACKAGE_DIR}"

find "${PROJECT_ROOT}/src" -name '*.java' -print0 \
    | xargs -0 javac -encoding UTF-8 -d "${OUT_DIR}"

cat > "${MANIFEST_FILE}" <<EOF
Main-Class: clinic.ClinicApp

EOF

jar --create --file "${JAR_FILE}" --manifest "${MANIFEST_FILE}" -C "${OUT_DIR}" .

cp "${PROJECT_ROOT}/README.md" "${PACKAGE_DIR}/README.md"
cp "${PROJECT_ROOT}/docs/系统设计说明.md" "${PACKAGE_DIR}/系统设计说明.md"
rsync -a --exclude '.DS_Store' "${PROJECT_ROOT}/data" "${PACKAGE_DIR}/"

cat > "${PACKAGE_DIR}/RUN.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$(ls "${SCRIPT_DIR}"/clinic-app-*.jar | head -n 1)"
if [ ! -f "${JAR_PATH}" ]; then
    echo "Jar file not found. Did you run the release packaging script?" >&2
    exit 1
fi
exec java -jar "${JAR_PATH}"
EOF
chmod +x "${PACKAGE_DIR}/RUN.sh"

cat > "${PACKAGE_DIR}/RUN.bat" <<'EOF'
@echo off
set SCRIPT_DIR=%~dp0
for %%f in ("%SCRIPT_DIR%clinic-app-*.jar") do set JAR_PATH=%%f
if not exist "%JAR_PATH%" (
    echo Jar file not found. Did you run the release packaging script?
    exit /b 1
)
java -jar "%JAR_PATH%"
EOF

( cd "${RELEASE_ROOT}" && zip -rq "${ZIP_FILE}" "clinic-app-${VERSION}" )

rm -f "${MANIFEST_FILE}"

echo "Release package created at ${ZIP_FILE}"