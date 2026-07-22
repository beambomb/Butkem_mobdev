# Product Requirement Document (PRD)
## Automated Energy Monitoring & Anomaly Detection System (AEMADS)

**PROJECT NAME:** Automated Energy Monitoring & Anomaly Detection System (AEMADS)  
**TARGET PLATFORM:** Android Mobile Application (Jetpack Compose)  
**TECH STACK:** Kotlin, Jetpack Compose, Supabase SDK (Auth & Realtime), Python Client  
**DOMAIN KASUS:** Pabrik Manufaktur Otomotif (Lini Stamping, Welding, Assembly)  
**DOKUMEN VERSI:** 4.0 (Unified Giant Document - High-Density Interval Automation)  

---

## Segmen 1: Executive Summary & Objective

AEMADS adalah aplikasi mobile berbasis Android yang dirancang khusus untuk memantau konsumsi energi kelistrikan industri serta mendeteksi anomali secara *real-time* pada pabrik manufaktur otomotif. 

Tujuan utama sistem ini adalah menghadirkan sistem deteksi dini (*early warning system*) yang bersifat mandiri dan lepas tangan (*hands-free*) saat proses demonstrasi operasional. Dengan mengganti perangkat fisik menggunakan **"Python Synthetic Data Generator"** berkecepatan tinggi, sistem secara dinamis menyuntikkan data dengan interval detik prima relatif (10 dan 13 detik) guna memicu simulasi anomali yang padat aksi tanpa risiko tabrakan data ganda di awal. 

Aplikasi mobile mengimplementasikan arsitektur dasbor tetap (*Fix Layout*) yang reaktif merespons 3 skenario kelistrikan pabrik dari bentuk visual grafiknya, serta dilengkapi dengan sistem notifikasi lokal dengan prioritas berlapis (*Tiered Notification*) demi keselamatan kerja operasional.

---

## Segmen 2: System Architecture & Real-Time Data Workflow

Sistem berjalan penuh secara *serverless frontend* menggunakan arsitektur *event-driven*:

1. **SIMULASI DATA (Python Edge Layer)**:
   Script Python berjalan mandiri menggunakan konsep *counter interval* waktu berbasis detik. Setiap 2 detik, script mengalkulasi metrik daya industri 3-Phase secara matematis dan mengirimkan datanya melalui REST API (HTTP POST) menuju tabel di cloud database.

2. **PENYIMPANAN DAN REPLIKASI (Supabase Cloud Layer)**:
   Supabase menerima *record* data baru pada tabel `energy_logs`. Dengan mengaktifkan fitur *Realtime Replication* (PostgreSQL WAL/Write-Ahead Logging via WebSockets), Supabase langsung memancarkan (*broadcast*) perubahan data tersebut ke seluruh klien internet yang terhubung dengan latensi rendah (di bawah 1 detik).

3. **KONSUMSI DATA & RENDERING (Android Application Layer)**:
   Aplikasi Jetpack Compose membuka koneksi *persistent* WebSocket via Supabase Kotlin SDK. Data dialirkan secara asinkron lewat Kotlin Coroutines Flow menuju `ViewModel`. `ViewModel` mengevaluasi angka terhadap batas aman (*Threshold*), memperbarui state UI secara reaktif, dan memerintahkan Android Notification Manager untuk memicu alarm lokal.

---

## Segmen 3: Fix Layout Integration & Visual Chart Requirements

Dasbor aplikasi menggunakan tata letak tetap (*Fix Layout*) yang secara konsisten menampilkan 3 chart utama di layar utama. Respons terhadap anomali tidak mengubah tata letak komponen, melainkan merubah karakteristik bentukan garis, jarum indikator, dan warna chart secara drastis.

### Component A: Live Trend Line Chart
- **Karakteristik**: Menampilkan pergerakan konsumsi daya aktif dalam satuan KiloWatt (kW). Menggunakan mekanisme antrean FIFO (First-In, First-Out) dengan kapasitas maksimal 20 data terakhir (Jendela waktu 40 detik) untuk menghemat RAM perangkat.

### Component B: Radial Gauge Chart
- **Karakteristik**: Berbentuk busur takometer setengah lingkaran (180 derajat) untuk memantau Faktor Daya / Power Factor (Cos Phi) dengan rentang nilai dari 0.0 hingga 1.0.

### Component C: Specific Energy Consumption (SEC) Bar Chart
- **Karakteristik**: Grafik batang vertikal yang mengukur akumulasi tingkat efisiensi energi per menit dengan rumus rasio: `kWh terpakai / Jumlah Output Komponen Mobil yang sukses`.

---

## Segmen 4: Automated Data Generator Scenario (Interval-Based 3 Cases Engine)

Script Python berjalan secara otomatis tanpa bergantung pada durasi menit. Baseline data secara default adalah CASE 1 (Normal). Anomali akan disuntikkan secara periodik menggunakan sistem *counter* detik terpisah untuk menciptakan simulasi yang padat aksi dan efisien saat demo:

- **Pemicu CASE 2 (Spike Anomaly)**: Terjadi tepat setiap kelipatan 10 detik.
- **Pemicu CASE 3 (Drift Anomaly)** : Terjadi tepat setiap kelipatan 13 detik.

> **Catatan**: Pola interval ini meminimalkan risiko tabrakan anomali ganda karena kelipatan persekutuan terkecil (KPK) berada jauh di detik ke-130.

### KONDISI DEFAULT: CASE 1 - OPERASI NORMAL (BASELINE)
- **Karakteristik Kasus**: Berjalan terus-menerus sebagai data dasar di detik-detik biasa.
- **Perilaku Angka**: Daya (kW) fluktuatif wajar di rentang 80kW - 110kW. Tegangan stabil 380V. Cos Phi tinggi (0.88 - 0.94). Output produksi stabil 5 unit/siklus.
- **Bentukan Visual**: 
  - **CH-A (Line)**: Garis horizontal bergerak lurus dengan riak gelombang tenang di bawah batas aman.
  - **CH-B (Gauge)**: Jarum kokoh menunjuk ke area kanan atas. Busur berwarna HIJAU PENUH.
  - **CH-C (Bar)**: Tinggi batang antar-menit seragam, pendek, dan konstan hemat.

### DI TRIGGER SETIAP 10 DETIK SEKALI: CASE 2 - LONJAKAN BEBAN AKUT (SPIKE ANOMALY)
- **Karakteristik Kasus**: Saat counter detik mencapai kelipatan 10 (detik 10, 20, 30, dst), script menyuntikkan 1 paket data lonjakan ekstrem, lalu langsung kembali ke normal.
- **Perilaku Angka**: Daya melonjak ekstrem ke 280kW, Tegangan drop ke 340V, Cos Phi anjlok ke 0.45, Output produksi = 0. (Hanya bertahan selama 1 siklus/2 detik).
- **Bentukan Visual**: 
  - **CH-A (Line)**: Garis menanjak tegak lurus membentuk pola "paku vertikal tajam" menembus garis Threshold, area lonjakan BERUBAH MERAH MENYALA, lalu langsung anjlok kembali.
  - **CH-B (Gauge)**: Jarum terbanting drastis ke arah kiri bawah (angka 0.40). Seluruh komponen radial chart berubah menjadi MERAH TOTAL DAN BERKEDIP (BLINKING).
  - **CH-C (Bar)**: Mengalami kekosongan visual (Bar terputus/Drop ke nol) pada menit/siklus tersebut.

### DI TRIGGER SETIAP 13 DETIK SEKALI: CASE 3 - DEGRADASI MESIN (DRIFT ANOMALY)
- **Karakteristik Kasus**: Saat counter detik mencapai kelipatan 13 (detik 13, 26, 39, dst), script memicu efek degradasi yang bertahan selama 3 siklus berturut-turut (total 6 detik).
- **Perilaku Angka**: Dalam jangka waktu pemicu tersebut, setiap siklus data baru dikirim, nilai daya mendapat akumulasi peningkatan linier (Siklus 1: +15kW, Siklus 2: +30kW, Siklus 3: +45kW). Cos Phi menurun konstan 0.04 per siklus.
- **Bentukan Visual**: 
  - **CH-A (Line)**: Garis grafik MEMBENTUK POLA DIAGONAL YANG MERANGKAK NAIK secara konstan dalam 3 data berurutan hingga memotong garis batas aman secara perlahan.
  - **CH-B (Gauge)**: Jarum merayap turun perlahan ke kiri (0.80 -> 0.75). Busur warna bertransisi statis dari Hijau -> KUNING -> ORANYE (Warning Status).
  - **CH-C (Bar)**: Batang grafik membentuk POLA ANAK TANGGA YANG SEMAKIN MENINGGI setiap menitnya.

---

## Segmen 5: Tiered Notification System & Threshold Logic

Aplikasi Android mengevaluasi setiap payload *real-time* dari WebSocket Supabase dan memicu sistem notifikasi lokal sesuai dengan tingkat keparahan kasus:

### 1. RESPONS NOTIFIKASI - CASE 1 (NORMAL):
- **Logic Trigger**: `power_kw <= 150` dan trend data stabil.
- **Mekanisme**: Tidak memicu Android System Notification.
- **Status UI**: Dasbor menampilkan badge teks hijau kecil: *"System Health: Operational"*.

### 2. RESPONS NOTIFIKASI - CASE 2 (CRITICAL SPIKE):
- **Logic Trigger**: `power_kw > 150` dengan akselerasi instan (data melompat radikal dari siklus sebelumnya).
- **Mekanisme**: Memicu Android Notification Manager dengan status **HIGH PRIORITY (Heads-Up)**. Perangkat wajib bergetar panjang dan membunyikan suara alarm pendek bawaan sistem.
- **Konten Teks**: 🚨 *"[CRITICAL] Spike Anomaly Detected on Lini Stamping! Power reached 280kW. Mechanical overload/jam suspected. Inspection required immediately!"*

### 3. RESPONS NOTIFIKASI - CASE 3 (MAINTENANCE DRIFT):
- **Logic Trigger**: Evaluasi array FIFO menunjukkan tren kenaikan positif konstan selama 3 data berturut-turut yang dipicu oleh siklus interval 13 detik.
- **Mekanisme**: Memicu Android Notification Manager dengan status **DEFAULT PRIORITY (Silent)**. Notifikasi muncul di bar atas sistem secara pasif tanpa suara alarm yang mengejutkan.
- **Konten Teks**: ⚠️ *"[MAINTENANCE ALERT] Drift Anomaly Detected on Lini Welding. Gradual efficiency loss observed over the last 3 cycles. Schedule maintenance routine."*

---

## Segmen 6: Database Schema (Supabase PostgreSQL Requirements)

Aplikasi mobile terhubung via WebSocket Supabase Client. Agar data terpancar secara *real-time*, fitur Replication (WAL) wajib diaktifkan pada tabel berikut via dashboard Supabase:

**Nama Tabel**: `energy_logs`

| Nama Kolom | Tipe Data | Keterangan / Spesifikasi Fungsi |
| :--- | :--- | :--- |
| `id` | BIGINT (PK) | Auto Increment, Identity |
| `lini_name` | VARCHAR | Nama Lini Produksi (Stamping, Welding, Assembly) |
| `power_kw` | FLOAT | Nilai Daya Aktif berjalan (Sumber Input untuk Chart A) |
| `power_factor` | FLOAT | Nilai Cos Phi rentang 0.0 - 1.0 (Sumber untuk Chart B) |
| `output_qty` | INT | Jumlah komponen sukses diproduksi pada siklus tersebut |
| `sec_val` | FLOAT | Rasio Efisiensi: `power_kw / output_qty` (Untuk Chart C) |
| `created_at` | TIMESTAMPTZ | Timestamp data masuk secara otomatis via `now()` |

---

## Segmen 7: User Acceptance Testing (UAT) Matrix

| ID Uji | Skenario Uji | Langkah Aksi | Hasil Akhir yang Diharapkan |
| :--- | :--- | :--- | :--- |
| **UAT-01** | Pengujian Otomatis Fase Baseline (Case 1 Validation) | Jalankan Python Engine, amati data pada detik-detik biasa (bukan kelipatan 10/13) | Dasbor menampilkan visualisasi mendatar aman, jarum di area hijau, tidak ada notifikasi. |
| **UAT-02** | Pengujian Otomatis Interval 10 Detik (Case 2 Validation) | Tunggu hingga counter waktu mencapai kelipatan 10 detik (detik 10, 20, 30, dst). | Grafik CH-A melompat merah, CH-B drop merah berkedip, HP bergetar & alarm Heads-Up aktif |
| **UAT-03** | Pengujian Otomatis Interval 13 Detik (Case 3 Validation) | Tunggu hingga counter waktu mencapai kelipatan 13 detik (detik 13, 26, 39, dst). | Garis CH-A naik miring, jarum CH-B ke oranye, CH-C membentuk tangga, notifikasi silent muncul |
