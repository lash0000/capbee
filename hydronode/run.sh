#!/usr/bin/env bash
# Compile and run Hydronode with the plain JDK toolchain (no build system).
set -euo pipefail

cd "$(dirname "$0")"

OUT_DIR="out"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

mapfile -t SOURCES < <(find src -name '*.java' | sort)

javac -Xlint:all -d "$OUT_DIR" "${SOURCES[@]}"
java -cp "$OUT_DIR" com.hydronode.Main "$@"
