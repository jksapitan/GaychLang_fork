#!/usr/bin/env bash
# Builds dist/GaychLang.jar  (needs a JDK 17+ : javac and jar on PATH)
set -euo pipefail
cd "$(dirname "$0")"

JAVAC="${JAVAC:-javac}"
JAR="${JAR:-jar}"

rm -rf out dist
mkdir -p out dist
"$JAVAC" --release 17 -encoding UTF-8 -d out src/gaych/*.java
mkdir -p out/examples
cp examples/*.gaych out/examples/
"$JAR" --create --file dist/GaychLang.jar --main-class gaych.Main -C out .
echo "Built dist/GaychLang.jar"
echo "Run the IDE:      java -jar dist/GaychLang.jar"
echo "Run a program:    java -jar dist/GaychLang.jar examples/hello.gaych"
