#!/usr/bin/env sh
# Minimal wrapper to download/run gradle
set -e
WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS="$WRAPPER_DIR/gradle-wrapper.properties"
DIST_URL="$(sed -n 's/^distributionUrl=\(.*\)$/\1/p' "$PROPS")"
DIST_FILE="${DIST_URL##*/}"
CACHE="${HOME}/.gradle/wrapper/dists/${DIST_FILE%.zip}"
GRADLE_DIR="$(dirname "$WRAPPER_DIR")/.."
mkdir -p "$CACHE"
if [ ! -d "$CACHE/gradle" ]; then
  TMP="$(mktemp -d)"
  curl -fsSL "$DIST_URL" -o "$TMP/gradle.zip"
  mkdir -p "$CACHE" && unzip -q "$TMP/gradle.zip" -d "$CACHE"
fi
exec "$CACHE"/gradle-*/bin/gradle "$@"
