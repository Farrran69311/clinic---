#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-123456}
MYSQL_DATABASE=${MYSQL_DATABASE:-clinic}
DATA_DIR=${DATA_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../data" && pwd)}

if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql 命令未找到，请安装 MySQL 客户端或配置 PATH" >&2
    exit 1
fi

TABLES=(
    users
    patients
    doctors
    appointments
    consultations
    medicines
    prescriptions
    expert_sessions
    expert_participants
    meeting_minutes
    expert_advices
    case_library
    work_progress
    calendar_events
    payments
    insurance_claims
    stock_movements
    audit_logs
)

mysql \
    --host="${MYSQL_HOST}" \
    --port="${MYSQL_PORT}" \
    --user="${MYSQL_USER}" \
    --password="${MYSQL_PASSWORD}" \
    --local-infile=1 \
    -e "SET GLOBAL local_infile = 1;" >/dev/null 2>&1 || true

for table in "${TABLES[@]}"; do
    CSV_FILE="${DATA_DIR}/${table}.csv"
    if [ ! -f "${CSV_FILE}" ]; then
        echo "跳过 ${table}：未找到 ${CSV_FILE}" >&2
        continue
    fi
    echo "导入 ${CSV_FILE} -> ${MYSQL_DATABASE}.${table}"
    header=$(head -n 1 "${CSV_FILE}")
    columns=$(echo "${header}" | sed 's/`/``/g; s/|/`,`/g; s/^/`/; s/$/`/')
    escaped_path=$(printf "%s" "${CSV_FILE}" | sed "s/'/''/g")
    sql=$(cat <<SQL
    LOAD DATA LOCAL INFILE '${escaped_path}' INTO TABLE \`${MYSQL_DATABASE}\`.\`${table}\`
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '|'
LINES TERMINATED BY '\n'
IGNORE 1 LINES (${columns});
SQL
    )
    mysql \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT}" \
        --user="${MYSQL_USER}" \
        --password="${MYSQL_PASSWORD}" \
        --local-infile=1 \
        --default-character-set=utf8mb4 \
        -e "${sql}"
    echo "完成 ${table}"
    echo
done

echo "全部导入完成"
