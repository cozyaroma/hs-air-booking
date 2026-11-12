#!/bin/sh
# macOS/Linux: 컴파일 후 프로그램 실행 (JDK 17 이상)
set -e
cd "$(dirname "$0")"
sh ./build.sh
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.app.Main
