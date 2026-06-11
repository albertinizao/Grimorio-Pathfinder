#!/usr/bin/env sh

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_CMD="$SCRIPT_DIR/.tools/apache-maven-3.9.9/bin/mvn"

if [ -x "$MVN_CMD" ]; then
  exec "$MVN_CMD" "$@"
fi

exec mvn "$@"
