@echo off
title SFRD v8.9.4 - Build & Install ke HP
color 0A

cd /d "c:\Users\dhavi\antigravity\SFRD"

echo ========================================================
echo   SFRD v8.9.4: Build & Install ke Perangkat HP
echo ========================================================
echo.

echo [1/3] Menjalankan Gradle Assemble Debug...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build Gradle gagal dengan error code %ERRORLEVEL%.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Menyalin APK ke root folder...
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "SFRD-v8.9.4.apk"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "RWC_Media_Production_v8.9.4.apk"

echo.
echo [3/3] Menginstall APK ke HP via ADB...
call adb install -r "app\build\outputs\apk\debug\app-debug.apk"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [WARN] ADB Install gagal. Pastikan HP terhubung via kabel USB dan USB Debugging aktif.
) else (
    echo.
    echo ========================================================
    echo   SUKSES TERINSTALL DI HP (v8.9.4)!
    echo ========================================================
)

echo.
pause
