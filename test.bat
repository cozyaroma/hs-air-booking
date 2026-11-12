@echo off
chcp 65001 > nul
rem Windows: 전체 자동 테스트 (공용 + A + B + C + 페이지 스모크 테스트)
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -sourcepath "src;test" -d out test\airline\AllTests.java src\airline\app\Main.java src\airline\dev\PageLauncher.java src\airline\dev\ContextPack.java
if errorlevel 1 (
  echo [실패] 컴파일 오류가 있습니다.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.AllTests
if errorlevel 1 (
  echo [실패] 테스트가 실패했습니다. 위의 FAIL 줄을 확인하세요.
  pause
  exit /b 1
)
echo [통과] 모든 테스트를 통과했습니다.
pause
