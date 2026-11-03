#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out/test-classes
mkdir -p out/test-classes
find src test -name '*.java' > out/test-sources.txt
javac -encoding UTF-8 -d out/test-classes @out/test-sources.txt
java -ea -cp out/test-classes asmquest.vm.VirtualMachineTest
java -ea -Djava.awt.headless=true -cp out/test-classes asmquest.ui.UiSmokeTest
