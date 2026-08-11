@echo off
title SFRD v8.8.4 - Git Push + Build APK
color 0B

cd /d "c:\Users\dhavi\antigravity\SFRD"

echo =====================================
echo   STEP 1: Git Push ke GitHub
echo =====================================
echo.

git add -A
git commit -m "v8.8.4: Fix cache validation performance issue causing slow web loading"
git push

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [WARN] Git push mungkin gagal atau tidak ada perubahan baru. Lanjut ke build...
)

echo.
echo =====================================
echo   STEP 2: Build APK v8.8.4
echo =====================================
echo.

call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD GAGAL! Error code: %ERRORLEVEL%
    pause
    exit /b 1
)

echo.
echo [STEP 3] Copy APK ke folder root...
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "SFRD-v8.8.4.apk"

echo.
echo =====================================
echo   SELESAI!
echo   APK: SFRD-v8.8.4.apk
echo =====================================
echo.
pause
