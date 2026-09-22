@echo off
title SFRD v8.9.1 - Install ke HP via USB
color 0A

cd /d "c:\Users\dhavi\antigravity\SFRD"

echo =====================================
echo   Install APK v8.9.1 ke HP via USB
echo =====================================
echo.

adb devices
echo.
echo Menginstall SFRD-v8.9.1.apk ke HP...
adb install -r "SFRD-v8.9.1.apk"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Jika muncul error 'unauthorized', silakan cek layar HP Anda dan klik 'Izinkan / Allow USB debugging'.
    pause
    exit /b 1
)

echo.
echo =====================================
echo   SUKSES: v8.9.1 BERHASIL TERPASANG!
echo =====================================
pause
