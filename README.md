# TollScan — Scan Struk Tol Otomatis

Aplikasi Android (Kotlin + Jetpack Compose) untuk memindai struk tol, mengekstrak
**nama gerbang tol, tanggal, jam, dan tarif** secara otomatis dengan OCR on-device,
menyimpan riwayat transaksi, dan mengekspor laporan periode tertentu ke **Excel (.xlsx)**
dan **PDF** lengkap dengan lampiran foto struk aslinya.

## Fitur

- 📷 **Scan struk**: ambil foto lewat kamera atau pilih dari galeri.
- 🔎 **OCR on-device** (Google ML Kit Text Recognition) — tidak perlu koneksi internet saat memindai.
- 🧠 **Parser multi-format**: beberapa pola regex dicoba berurutan untuk gerbang tol, tanggal,
  jam, dan tarif, sehingga mendukung berbagai jenis/layout struk tol (Jasa Marga, Astra Tol, dll).
  Hasil OCR selalu ditampilkan dalam form yang bisa dikoreksi manual sebelum disimpan.
- 🩹 **Koreksi orientasi foto otomatis** (baca EXIF) supaya foto struk tidak terbalik.
- 🗂️ **Riwayat transaksi** tersimpan lokal (Room database) dengan ringkasan total pengeluaran.
- 📊 **Export Excel (.xlsx)** untuk rentang tanggal yang dipilih — dibuat dengan writer XLSX
  minimal tanpa dependency eksternal (menghindari masalah kompatibilitas Apache POI di Android).
- 📄 **Export PDF** berisi tabel ringkasan transaksi **+ lampiran foto struk asli per transaksi**.
- 🎨 UI Material 3 dengan tema biru–emas yang elegan, mode terang & gelap.

## Struktur Proyek

```
TollScan/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tollscan/app/
│       │   ├── MainActivity.kt
│       │   ├── TollScanApp.kt
│       │   ├── data/            # Room entity, DAO, database, repository
│       │   ├── ocr/             # ML Kit wrapper + parser multi-format
│       │   ├── export/          # XlsxWriter (custom) + PdfExporter (built-in PdfDocument)
│       │   ├── util/            # FileProvider & bitmap helpers
│       │   └── ui/
│       │       ├── theme/       # Warna, tipografi, Material3 theme
│       │       ├── navigation/  # NavGraph
│       │       └── screens/     # HomeScreen, ScanScreen, DetailScreen, ExportScreen
│       └── res/
├── .github/workflows/android-build.yml   # CI build otomatis -> APK debug
├── build.gradle.kts
└── settings.gradle.kts
```

## Cara Build

### Opsi 1 — GitHub Actions (paling mudah)
1. Push/upload folder ini ke repository GitHub baru.
2. Buka tab **Actions** di repo, workflow **"Android CI Build"** akan berjalan otomatis
   pada setiap push ke branch `main` (atau jalankan manual lewat "Run workflow").
3. Setelah selesai, unduh APK dari bagian **Artifacts** pada hasil run tersebut.

   Workflow ini menginstal Gradle 8.6 langsung lewat `gradle/actions/setup-gradle`,
   jadi **tidak memerlukan file `gradlew` / `gradle-wrapper.jar` biner** di repo.

### Opsi 2 — Android Studio (lokal)
1. Buka folder `TollScan` di Android Studio (versi terbaru, mendukung AGP 8.3+).
2. Android Studio akan otomatis membuatkan `gradlew` & `gradle-wrapper.jar` saat sinkronisasi
   pertama kali (klik "Sync Now" jika diminta).
3. Jalankan `Run > Run 'app'` untuk instal ke device/emulator, atau `Build > Build APK(s)`.

### Opsi 3 — Command line dengan Gradle terinstal
```bash
gradle assembleDebug
```
APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Cara Pakai

1. Tekan **Scan Struk** di layar utama.
2. Ambil foto struk tol (usahakan rata, cukup cahaya, tidak buram) atau pilih dari galeri.
3. Aplikasi otomatis membaca teks dan mengisi field **Gerbang Tol, Tanggal, Jam, Tarif** —
   periksa dan koreksi bila ada yang kurang tepat, lalu tekan **Simpan Struk**.
4. Semua struk yang tersimpan muncul di riwayat pada layar utama, lengkap dengan total pengeluaran.
5. Tekan ikon **Export** di pojok kanan atas, pilih rentang tanggal, lalu ekspor ke
   **Excel** atau **PDF** (PDF menyertakan lampiran foto tiap struk). File langsung bisa dibagikan
   lewat aplikasi lain (WhatsApp, email, Google Drive, dll).

## Catatan Jujur Tentang Kualitas & Verifikasi Build

Kode ini ditulis dengan cermat mengikuti API resmi Android/Jetpack Compose/ML Kit/Room,
namun ditulis di lingkungan **tanpa Android SDK dan tanpa akses internet**, sehingga
**belum bisa dijalankan `gradle build` secara langsung untuk verifikasi 100%** sebelum diserahkan.
Yang sudah dipastikan:
- Struktur Gradle, dependency, dan versi library dipilih dari versi stabil yang sudah lama beredar
  (AGP 8.3.2, Kotlin 1.9.24, Compose BOM 2024.06.00, Room 2.6.1, ML Kit Text Recognition 16.0.0).
- Fitur export (XLSX & PDF) sengaja dibuat **tanpa dependency eksternal** (hanya `java.util.zip`
  dan `android.graphics.pdf.PdfDocument` bawaan Android) supaya risiko kegagalan kompilasi
  akibat versi library pihak ketiga jauh lebih kecil.

Jika saat build pertama di GitHub Actions/Android Studio ternyata muncul error versi
(misalnya versi Kotlin/KSP/AGP yang lebih baru sudah tersedia), penyesuaian versi biasanya
cukup dengan mengubah angka versi di `build.gradle.kts` (root) — silakan beri tahu saya
pesan errornya dan saya bantu perbaiki.

## Format Struk yang Didukung

Parser (`ReceiptParser.kt`) mencoba beberapa pola sekaligus untuk tiap field, sehingga
tidak terpaku pada satu template struk saja:
- Nama gerbang: mendeteksi kata kunci `GERBANG TOL`, `GERBANG`, `GT.`, `GARDU`, `GATE`, `RUAS`, dll.
- Tanggal: mendukung format `DD/MM/YYYY`, `DD-MM-YYYY`, maupun `YYYY-MM-DD`.
- Jam: format `HH:MM` atau `HH:MM:SS`.
- Tarif: mendeteksi kata kunci `TARIF`, `TOTAL`, `BAYAR`, `JUMLAH`, atau angka setelah `RP`.

Karena kualitas OCR sangat bergantung pada kualitas foto, hasil ekstraksi **selalu**
ditampilkan dalam form yang bisa dikoreksi manual sebelum disimpan — memastikan data akhir
akurat walau format struknya belum pernah "dilihat" oleh parser sebelumnya.
