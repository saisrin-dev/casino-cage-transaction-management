#!/usr/bin/env sh
set -eu
if [ -e .env ]; then echo '.env exists; preserving encryption key and credentials.'; exit 1; fi
umask 077
printf 'DB_PASSWORD=%s\nDATA_ENCRYPTION_KEY=%s\nDEMO_PASSWORD=Demo-%s\nDEMO_SEED=true\nSIMULATION_CONTROLS=true\nFILING_MODE=mock\nCOOKIE_SECURE=false\n' "$(openssl rand -hex 24)" "$(openssl rand -base64 32)" "$(openssl rand -hex 12)" > .env
printf 'Created .env. Use DEMO_PASSWORD from that file. Never commit .env.\n'
