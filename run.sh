#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$ROOT/src/main/java"
OUT="$ROOT/out"
mkdir -p "$OUT"
find "$SRC" -name "*.java" > "$OUT/sources.txt"
javac -d "$OUT" @"$OUT/sources.txt"
java -cp "$OUT" braco96.robofab.FactoryApp
