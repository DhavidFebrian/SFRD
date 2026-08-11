@echo off
title SFRD v8.8.2 - Build APK
color 0B
echo =====================================
echo   SFRD Build APK v8.8.2
echo =====================================
echo.

cd /d "%~dp0"

echo [1/2] Building APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD GAGAL! Error code: %ERRORLEVEL%
    pause
    exit /b 1
)

echo.
echo [2/2] Copy APK ke folder SFRD root...
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "SFRD-v8.8.2.apk"

echo.
echo =====================================
echo   BUILD SELESAI! File: SFRD-v8.8.2.apk
echo =====================================
echo.
echo Changelog v8.8.2:
echo   - FIX: Deskripsi listing ID 12463 (dan semua ID lain) tidak lagi ngaco
echo   - FEATURE: Card Upload IG tampilkan judul dari notes spreadsheet
echo   - FEATURE: Nama file download foto menggunakan judul dari notes
echo.
pause
