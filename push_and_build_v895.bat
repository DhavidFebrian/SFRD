@echo off
title SFRD v8.9.5 - Git Push + Build APK
color 0B

cd /d "c:\Users\dhavi\antigravity\SFRD"

echo =====================================
echo   STEP 1: Git Push ke GitHub
echo =====================================
echo.

git add -A
git commit -m "v8.9.5: Fix ME cell highlight, meeting reload caching, category summary stats, and expand absensi scan row range"
git push

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [WARN] Git push mungkin gagal atau tidak ada perubahan baru. Lanjut ke build...
)

echo.
echo =====================================
echo   STEP 2: Build APK v8.9.5
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
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "RWC_Media_Production_v8.9.5.apk"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "SFRD-v8.9.5.apk"

echo.
echo [STEP 4] Menginstall ke HP via ADB...
call adb install -r "app\build\outputs\apk\debug\app-debug.apk"

echo.
echo =====================================
echo   SELESAI!
echo   APK 1: RWC_Media_Production_v8.9.5.apk
echo   APK 2: SFRD-v8.9.5.apk
echo =====================================
echo.
pause
