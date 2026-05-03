@echo off
echo Compiling MusicWave...
if not exist out mkdir out
javac -cp "lib\jlayer.jar" -d out src\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Launching MusicWave...
java -cp "out;lib\jlayer.jar" Main
