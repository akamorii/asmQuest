#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out/classes
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 -d out/classes @out/sources.txt
java -cp out/classes asmquest.AsmQuestApp
