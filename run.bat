@echo off
chcp 65001 > nul
rem Windows: 컴파일 후 프로그램 실행 (JDK 17 이상)
cd /d "%~dp0"
call :build || exit /b 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.app.Main
exit /b 0

:build
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\airline\app\Main.java
if errorlevel 1 (
  echo [실패] 컴파일 오류가 있습니다. 위 메시지를 확인하세요. JDK 17 이상이 필요합니다.
  pause
  exit /b 1
)
exit /b 0
