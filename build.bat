@echo off
REM Builds dist\GaychLang.jar  (needs a JDK 17+ : javac and jar on PATH)
cd /d "%~dp0"
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist
dir /s /b src\gaych\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out @sources.txt || (del sources.txt & exit /b 1)
del sources.txt
mkdir out\examples
copy examples\*.gaych out\examples\ >nul
jar --create --file dist\GaychLang.jar --main-class gaych.Main -C out . || exit /b 1
echo Built dist\GaychLang.jar
echo Run the IDE:    java -jar dist\GaychLang.jar
echo Run a program:  java -jar dist\GaychLang.jar examples\hello.gaych
