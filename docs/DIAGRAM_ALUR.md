# 📊 Diagram Alur Sistem SFRD (Schedule Foto RWC)

Dokumentasi diagram arsitektur dan alur kerja komprehensif untuk aplikasi **SFRD**.

---

## 1. 🌐 Arsitektur Sistem Global

Diagram berikut menggambarkan interaksi antara komponen aplikasi Android SFRD dengan service eksternal (Google Sheets, Portal Ray White, WhatsApp, GitHub OTA).

```mermaid
flowchart TB
    subgraph Client["📱 Android Client (SFRD App)"]
        UI["Jetpack Compose UI (Material 3)"]
        VM["ScheduleViewModel (StateFlow & Coroutines)"]
        Repo["ScheduleRepository & Local DB (Room / Coil Cache)"]
        PDF["Local PDF 3:4 Engine & Image Processor"]
        Face["ML Kit Face Recognition & PIN Helper"]
    end

    subgraph Backend["☁️ Google Workspace Backend"]
        GAS["Google Apps Script Web App (/exec)"]
        Sheets[("Google Sheets Database")]
    end

    subgraph External["🌍 Layanan Eksternal"]
        Portal["Ray White Cipete Web Portal"]
        GitHub["GitHub Releases API (Auto Update)"]
        WhatsApp["WhatsApp Business / Messenger"]
    end

    %% Client Internal Flow
    UI <-->|UI Event / State Binding| VM
    VM <-->|Data Request & Cache| Repo
    VM -->|Render & Export| PDF
    VM -->|Verify Attendance| Face

    %% External Connections
    Repo <-->|Bi-directional Sync via JSON REST| GAS
    GAS <-->|Read / Write Cells| Sheets
    Repo -->|Scrape Listing and Images| Portal
    Repo -->|Check Version and Download APK| GitHub
    PDF -->|Share PDF and Text Report| WhatsApp
```

---

## 2. 🔄 Alur Kerja Utama Aplikasi (End-to-End User Flow)

```mermaid
flowchart TD
    Start([Mulai Aplikasi]) --> Init[Inisialisasi Coil, Room DB, & Prefs]
    Init --> CheckUpdate{Cek Update GitHub?}
    
    CheckUpdate -- Ada Versi Baru --> PromptUpdate[Tampilkan Dialog Update & Download APK]
    PromptUpdate --> InstallAPK[Instalasi via FileProvider]
    
    CheckUpdate -- Versi Terkini --> CheckGAS{Apps Script URL Terkonfigurasi?}
    CheckGAS -- Belum --> OpenSetting[Buka Menu Setting & Input URL]
    OpenSetting --> CheckGAS
    
    CheckGAS -- Sudah --> MainNav[Pilih Menu Navigasi]

    %% Branching Menu
    MainNav --> NavMeeting[1. Weekly Meeting Workspace]
    MainNav --> NavMedia[2. Media & Image Downloader]
    MainNav --> NavSchedule[3. Scheduling Desk IG]
    MainNav --> NavAbsen[4. Absensi Kehadiran ME]
    MainNav --> NavNewsletter[5. Newsletter 3:4 PDF Generator]
    MainNav --> NavAnalytic[6. Ringkasan Analitik & WA Report]

    %% 1. Meeting
    NavMeeting --> InputListing[Input ID Listing / Auto-fetch Portal]
    InputListing --> SetTag[Pilih Tag: HOT / IG / FOTO ULANG & Catatan Edit]
    SetTag --> SaveMeeting[Simpan ke Google Sheets via GAS]

    %% 2. Media
    NavMedia --> FetchImages[Ambil Semua Foto Resolusi Tinggi dari Portal]
    FetchImages --> DownloadStorage[Simpan ke Galeri / Internal Storage]

    %% 3. Scheduling
    NavSchedule --> FilterSched[Filter Tab: Unscheduled / Scheduled]
    FilterSched --> PickDate[Pilih Tanggal via DatePicker Native]
    PickDate --> UpdateSched[Update Kolom Jadwal di Sheets]

    %% 4. Absen
    NavAbsen --> MethodAbsen{Pilih Metode Absen}
    MethodAbsen -- Face Scan / PIN --> ScanFace[Kamera & Face Detection / PIN Dialog]
    MethodAbsen -- Manual Centang --> CheckME[Centang Nama Marketing ME]
    ScanFace --> SyncAbsen[Sinkronisasi Status Hadir ke Sheets]
    CheckME --> SyncAbsen

    %% 5. Newsletter
    NavNewsletter --> InputPDF[Input Link PDF Portal]
    InputPDF --> ProcessPDF[Proses Crop 3:4 Portrait + Cover HD + Link Hyperlink]
    ProcessPDF --> SharePDF[Bagikan PDF ke WhatsApp]

    %% 6. Analytic
    NavAnalytic --> CalcMetric[Hitung Distribusi Target & Sesi Foto/Video/Drone]
    CalcMetric --> GenFormat[Format Teks Laporan Otomatis]
    GenFormat --> ShareWA[Kirim Teks ke WhatsApp Group]
```

---

## 3. ⏱️ Sequence Diagram: Sinkronisasi Dua Arah (Bi-directional Sync)

```mermaid
sequenceDiagram
    autonumber
    actor User as Pengguna (Marketing / Admin)
    participant UI as Compose Screen
    participant VM as ScheduleViewModel
    participant Repo as ScheduleRepository
    participant GAS as Google Apps Script API
    participant Sheets as Google Sheets DB

    %% Read Sync
    User->>UI: Buka Aplikasi / Pull to Refresh
    UI->>VM: loadMeetingData()
    VM->>Repo: fetchSchedules()
    Repo->>GAS: HTTP GET /exec?action=getData
    GAS->>Sheets: Read Range data baris & kolom
    Sheets-->>GAS: Return Rows Data
    GAS-->>Repo: JSON Array (Listing, ME, Catatan, Jadwal, Absen)
    Repo-->>VM: Emit StateFlow (Success)
    VM-->>UI: Render Daftar Listing & Status

    %% Write Sync
    User->>UI: Tambah / Edit Listing Properti
    UI->>VM: saveListing(data)
    VM->>Repo: pushListing(data)
    Repo->>GAS: HTTP POST /exec (Action: saveRow, payload: JSON)
    GAS->>Sheets: Update / Append baris
    Sheets-->>GAS: Success
    GAS-->>Repo: 200 OK (Status: updated)
    Repo-->>VM: Update Local Cache & UI State
    VM-->>UI: Tampilkan Notifikasi Berhasil Simpan
```

---

## 4. 📄 Sequence Diagram: Pemrosesan & Konversi Newsletter 3:4 PDF

```mermaid
sequenceDiagram
    autonumber
    actor User as Tim Marketing
    participant UI as NewsletterDialog
    participant Downloader as NewsletterDownloader
    participant Engine as Local PDF Processor
    participant WA as WhatsApp App

    User->>UI: Masukkan URL Brosur PDF Portal
    UI->>Downloader: startDownloadAndConvert(url)
    Downloader->>Downloader: Download raw PDF file ke cache
    Downloader->>Engine: convertA4ToPortrait34(pdfBytes)
    
    rect rgb(30, 40, 60)
        Note over Engine: 1. Render halaman PDF asli ke Bitmap HD<br/>2. Stretch pixel sisi kiri & kanan (Bebas distorsi)<br/>3. Hitung posisi teks cover & background dinamis<br/>4. Sisipkan interactive link overlay di atas gambar
    end
    
    Engine-->>Downloader: File PDF Baru (3:4 Portrait HD)
    Downloader-->>UI: File Uri siap dibagikan
    UI->>WA: Intent ACTION_SEND (MIME: application/pdf)
    WA-->>User: Tampilkan dialog kirim ke kontak/grup WhatsApp
```
