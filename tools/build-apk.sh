#!/usr/bin/env bash
# Builds an APK and files it in builds/ (git-ignored), named after the version.
#
#   tools/build-apk.sh            release build (default, signed with the key in keystore.properties)
#   tools/build-apk.sh debug      debug build
#
# The version comes from the VERSION file in the repo root (MAJOR.MINOR.PATCH).
# Output: builds/YBP-<version>.apk for release, builds/YBP-<version>-debug.apk for debug.
# Building the same version again replaces the file, so bump VERSION when you want to keep the old one.
set -euo pipefail
cd "$(dirname "$0")/.."

variant="${1:-release}"
case "$variant" in
  release) task="assembleRelease" ;;
  debug) task="assembleDebug" ;;
  *) echo "usage: $0 [release|debug]" >&2; exit 1 ;;
esac

if [ "$variant" = "release" ] && [ ! -f keystore.properties ]; then
  echo "keystore.properties is missing, so the release APK would be unsigned and uninstallable. See AGENTS.md." >&2
  exit 1
fi

./gradlew --console=plain ":app:$task"

apk="app/build/outputs/apk/$variant/app-$variant.apk"
[ -f "$apk" ] || { echo "expected $apk but it does not exist" >&2; exit 1; }

version=$(tr -d '[:space:]' < VERSION)
mkdir -p builds
suffix=""
[ "$variant" = "debug" ] && suffix="-debug"
out="builds/YBP-$version$suffix.apk"
cp "$apk" "$out"
echo
echo "Built: $out"
