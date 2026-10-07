#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ ! -f "$root/ui-automation/.env" ]; then
 umask 077
 printf 'DB_PASSWORD=%s\nDB_ROOT_PASSWORD=%s\nUI_ADMIN_USER=qa_%s\nUI_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$(openssl rand -hex 4)" "$(openssl rand -hex 24)" > "$root/ui-automation/.env"
fi
set -a
source "$root/ui-automation/.env"
set +a
