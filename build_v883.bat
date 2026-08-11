@echo off
title SFRD v8.8.3 - Build APK
color 0B
echo =====================================
echo   SFRD Build APK v8.8.3
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
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "SFRD-v8.8.3.apk"

echo.
echo =====================================
echo   BUILD SELESAI! File: SFRD-v8.8.3.apk
echo =====================================
echo.
echo Changelog v8.8.3:
echo   - FIX: Template foto di halaman Posts tampilkan judul dari notes spreadsheet
echo   - FEATURE: Halaman Scheduling Unscheduled dibagi 3 bagian:
echo       Turun Harga / Trade Area Jakarta Selatan / IG (lainnya)
echo   - FEATURE: Card unscheduled layout 2 kolom
echo   - FEATURE: Klik card unscheduled = popup detail (deskripsi + status posting)
echo   - FEATURE: Search dan Bulan di Scheduling Desk bisa expand/collapse (default collapse)
echo   - FIX: Halaman Upload IG - urutan ascending (terlama di atas)
echo.
pause
