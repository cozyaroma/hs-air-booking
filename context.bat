@echo off
chcp 65001 > nul
rem Windows: AI 채팅에 첨부할 작업 자료 묶음 만들기
rem   사용법: context.bat A   (A, B, C 중 하나) → context-pack-A.md 생성
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\airline\dev\ContextPack.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out airline.dev.ContextPack %1
pause
