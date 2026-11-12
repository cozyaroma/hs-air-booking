#!/bin/sh
# macOS/Linux: 페이지 하나만 바로 열기 (개발 모드)
#   사용법: ./page.sh SEAT ROUNDTRIP
set -e
cd "$(dirname "$0")"
sh ./build.sh
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.dev.PageLauncher "$@"
