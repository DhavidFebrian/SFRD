# SFRD v8.8.2 Build & Install Script
# Double-click atau jalankan di PowerShell: .\build_install_v882.ps1

$ErrorActionPreference = "Continue"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  SFRD Build & Install v8.8.2" -ForegroundColor Cyan  
Write-Host "  Fix: deskripsi listing + notes judul" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build APK
Write-Host "[1/2] Building APK Debug..." -ForegroundColor Yellow
$buildProcess = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c gradlew.bat assembleDebug" `
    -WorkingDirectory $scriptDir `
    -Wait -PassThru -NoNewWindow

if ($buildProcess.ExitCode -ne 0) {
    Write-Host "BUILD GAGAL! Exit code: $($buildProcess.ExitCode)" -ForegroundColor Red
    Write-Host "Coba jalankan secara manual: gradlew.bat assembleDebug" -ForegroundColor Yellow
    Read-Host "Tekan Enter untuk keluar"
    exit 1
}

$apkPath = Join-Path $scriptDir "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "APK tidak ditemukan di: $apkPath" -ForegroundColor Red
    Read-Host "Tekan Enter untuk keluar"
    exit 1
}

Write-Host "BUILD BERHASIL!" -ForegroundColor Green
$apkInfo = Get-Item $apkPath
Write-Host "APK: $([math]::Round($apkInfo.Length / 1MB, 1)) MB, dibuat: $($apkInfo.LastWriteTime)" -ForegroundColor Gray
Write-Host ""

# Step 2: Install ke HP
Write-Host "[2/2] Installing ke HP via ADB..." -ForegroundColor Yellow
Write-Host "Pastikan HP terhubung dan USB debugging aktif!" -ForegroundColor DarkYellow

$adbCheck = Start-Process -FilePath "adb" -ArgumentList "devices" -Wait -PassThru -NoNewWindow 2>$null
if ($adbCheck.ExitCode -ne 0) {
    Write-Host "ADB tidak ditemukan atau HP tidak terhubung." -ForegroundColor Red
    Write-Host "APK tersedia di: $apkPath" -ForegroundColor Yellow
    Write-Host "Install manual: adb install -r `"$apkPath`"" -ForegroundColor Gray
    Read-Host "Tekan Enter untuk keluar"
    exit 1
}

$installProcess = Start-Process -FilePath "adb" `
    -ArgumentList "install -r `"$apkPath`"" `
    -Wait -PassThru -NoNewWindow

if ($installProcess.ExitCode -eq 0) {
    Write-Host "" 
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "  INSTALL BERHASIL! v8.8.2 terpasang" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Changelog v8.8.2:" -ForegroundColor Cyan
    Write-Host "  - [FIX] Deskripsi listing tidak lagi berbeda dengan web" -ForegroundColor White
    Write-Host "    (termasuk ID 12463 luas tanah 125m2 sudah benar)" -ForegroundColor Gray
    Write-Host "  - [FEATURE] Card Upload IG sekarang tampilkan judul dari" -ForegroundColor White
    Write-Host "    kolom catatan spreadsheet jika ada 'judul XXX'" -ForegroundColor Gray
    Write-Host "  - [FEATURE] Nama file foto yang didownload sekarang" -ForegroundColor White
    Write-Host "    menggunakan judul dari notes atau scraping website" -ForegroundColor Gray
} else {
    Write-Host "Install gagal! Exit code: $($installProcess.ExitCode)" -ForegroundColor Red
    Write-Host "Coba manual: adb install -r `"$apkPath`"" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Tekan Enter untuk keluar"
