#!/usr/bin/env bash
# ============================================================
# 码上记 CodeInterview · MySQL 每日全量备份（T18）
#
# 用法：
#   bash scripts/backup.sh
# 行为：
#   1) mysqldump 全量导出到 backups/<db>_<yyyyMMdd_HHmmss>.sql.gz
#   2) 只删除 backups/ 目录内超过 14 天的 <db>_*.sql.gz（不碰其他文件）
# 可覆盖的环境变量：
#   DB_HOST / DB_PORT / DB_USERNAME / DB_PASSWORD / DB_NAME
#   BACKUP_DIR（默认仓库根 backups/）/ RETENTION_DAYS（默认 14）
#   MYSQLDUMP_BIN（默认自动查找 PATH 与本机 Windows MySQL 8.0 路径）
# ============================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-${REPO_ROOT}/backups}"
DB_NAME="${DB_NAME:-codeinterview}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

find_mysqldump() {
  if [[ -n "${MYSQLDUMP_BIN:-}" ]]; then
    printf '%s\n' "${MYSQLDUMP_BIN}"
    return 0
  fi
  if command -v mysqldump >/dev/null 2>&1; then
    command -v mysqldump
    return 0
  fi
  # 本机 Windows MySQL 8.0（WSL 下是 /mnt/d/...，Git Bash 下是 /d/...，两种都试）
  local candidates=(
    "/mnt/d/Develop/MySQL/MySQLServer8.0/bin/mysqldump.exe"
    "/d/Develop/MySQL/MySQLServer8.0/bin/mysqldump.exe"
    "D:/Develop/MySQL/MySQLServer8.0/bin/mysqldump.exe"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -x "${candidate}" ]]; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
  return 1
}

DUMP_BIN="$(find_mysqldump)" || {
  echo "ERROR: 找不到 mysqldump，请把 MYSQLDUMP_BIN 指到 mysqldump 可执行文件" >&2
  exit 1
}

mkdir -p "${BACKUP_DIR}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="${BACKUP_DIR}/${DB_NAME}_${STAMP}.sql.gz"

echo "[backup] dump ${DB_USERNAME}@${DB_HOST}:${DB_PORT}/${DB_NAME} -> ${OUT_FILE}"
# 密码走命令行会告警，重定向 stderr 屏蔽；--single-transaction 保证一致性快照且不锁表
"${DUMP_BIN}" \
  --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USERNAME}" --password="${DB_PASSWORD}" \
  --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers --no-tablespaces \
  "${DB_NAME}" 2>/dev/null | gzip -c > "${OUT_FILE}"

if [[ ! -s "${OUT_FILE}" ]]; then
  echo "ERROR: 备份文件为空，导出失败" >&2
  exit 1
fi

# 保留策略：只清理本目录内、本库前缀、超过 RETENTION_DAYS 天的文件
find "${BACKUP_DIR}" -maxdepth 1 -type f -name "${DB_NAME}_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete

echo "[backup] done: $(ls -lh "${OUT_FILE}" | awk '{print $5, $9}')"
