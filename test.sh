#!/bin/sh
# macOS/Linux: 전체 자동 테스트 (공용 + A + B + C + 페이지 스모크 테스트)
set -e
cd "$(dirname "$0")"
sh ./build.sh
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.AllTests
