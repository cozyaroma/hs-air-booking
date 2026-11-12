@echo off
chcp 65001 > nul
rem Windows: 페이지 하나만 바로 열기 (개발 모드)
rem   사용법: page.bat SEAT ROUNDTRIP
rem   페이지: SEARCH FLIGHT_SELECT PASSENGER SEAT PAYMENT CONFIRMATION MY_BOOKINGS
rem   데이터: ONEWAY ROUNDTRIP INTL INTL_ROUNDTRIP
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\airline\dev\PageLauncher.java
if errorlevel 1 (
  echo [실패] 컴파일 오류가 있습니다.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.dev.PageLauncher %1 %2
