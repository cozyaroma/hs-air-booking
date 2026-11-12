#!/bin/sh
# 공통 빌드 (run.sh, test.sh, page.sh, context.sh가 사용)
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src test -name "*.java")
