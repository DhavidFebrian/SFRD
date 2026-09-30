# SFRD v8.9.3 - Build & Install PowerShell Script
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  SFRD v8.9.3: Build & Install ke Perangkat HP" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -Path "c:\Users\dhavi\antigravity\SFRD"

Write-Host "[1/3] Menjalankan Gradle Assemble Debug..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build Gradle gagal dengan error code $LASTEXITCODE." -ForegroundColor Red
    Exit $LASTEXITCODE
}

Write-Host ""
Write-Host "[2/3] Menyalin APK ke root folder..." -ForegroundColor Yellow
Copy-Item -Path "app\build\outputs\apk\debug\app-debug.apk" -Destination "SFRD-v8.9.3.apk" -Force
Copy-Item -Path "app\build\outputs\apk\debug\app-debug.apk" -Destination "RWC_Media_Production_v8.9.3.apk" -Force

Write-Host ""
Write-Host "[3/3] Menginstall APK ke HP via ADB..." -ForegroundColor Yellow
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[WARN] ADB Install gagal. Pastikan HP terhubung via kabel USB dan USB Debugging aktif." -ForegroundColor Red
} else {
    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  SUKSES TERINSTALL DI HP (v8.9.3)!" -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
}
