#!/bin/sh
# macOS/Linux: AI 채팅에 첨부할 작업 자료 묶음 만들기
#   사용법: ./context.sh A   → context-pack-A.md 생성
set -e
cd "$(dirname "$0")"
sh ./build.sh
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.dev.ContextPack "$@"
