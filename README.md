# ReMinecraft

Server Minecraft hybrid yang mendukung pemain **Java Edition** dan **Bedrock Edition** secara bersamaan, berbasis Purpur (Paper fork) dengan Geyser + Floodgate.

---

## Persyaratan

Sebelum memulai, pastikan semua ini sudah terinstall:

| Kebutuhan | Versi | Keterangan |
|-----------|-------|------------|
| Java (JDK) | 21+ | Wajib. Disarankan JDK 25 di `jdk-25/` |
| Apache Maven | 3.9+ | Di `apache-maven-3.9.16/` atau install manual |
| CMake | 3.20+ | Untuk build C++ native DLL |
| Visual Studio Build Tools | 2019+ | MSVC compiler untuk CMake (Windows) |
| Bun | Latest | Untuk sidecar scripting (`source/bun/`) |

---

## Setup Pertama Kali

Ikuti langkah ini secara berurutan:

### 1. Siapkan Server JAR

Letakkan file `reminecraft-server.jar` (Purpur) di folder root (`C:\reminecraft\`).  
Download dari [purpurmc.org](https://purpurmc.org) sesuai versi MC yang digunakan.

### 2. Siapkan Plugin JARs

Letakkan file JAR plugin berikut di folder `plugin\`:

```
plugin\
  AuthMe.jar
  FastLogin.jar
  floodgate-spigot.jar
  Geyser-Spigot.jar
  LuckPerms.jar
  ProtocolLib.jar
```

Plugin bisa di-build dari source menggunakan script di `builder\` (lihat bagian Builder di bawah), atau download manual dari release masing-masing.

### 3. Jalankan Setup

```bat
builder\setuper.bat
```

Script ini akan:
- Memverifikasi Java tersedia
- Membuat folder `runfolder\` dan subfolder yang diperlukan
- Menyalin semua plugin dari `plugin\` ke `runfolder\plugins\`
- Membuat `eula.txt` secara otomatis
- Membuat `server.properties` default jika belum ada
- Membuat `config.json` template jika belum ada

### 4. Build Core Plugin

```bat
builder\buildcore.bat
```

Mengompilasi `ReminecraftCore` (plugin utama ReMinecraft) dan mendeploy JAR ke `runfolder\plugins\`.

### 5. (Opsional) Build Native DLL

```bat
builder\buildnative.bat
```

Mengompilasi library C++ (`reminecraft_native.dll`) untuk kompresi paket menggunakan Windows Compression API (MSZIP). Butuh CMake dan MSVC.

### 6. Jalankan Server

- **Development / Localhost:** `localhost.bat`
- **Production:** `runner.bat`

---

## Penjelasan Script

### `localhost.bat`
Menjalankan server untuk **testing di lokal**. Konfigurasi:
- RAM: `3G–4G` dengan **ZGC** (low-latency garbage collector)
- Flag: `-Dreminecraft.localhost=true`
- Otomatis menyalin plugin dari `plugin\` ke `runfolder\plugins\` sebelum start

Gunakan ini saat development atau mencoba fitur baru.

---

### `runner.bat`
Menjalankan server untuk **production/deployment**. Konfigurasi:
- RAM: `2G–4G` dengan **G1GC** (throughput-optimized, cocok untuk banyak player)
- JVM flags dioptimasi untuk performa: heap region size, GC tuning, dll
- Sama seperti localhost.bat tapi lebih hemat RAM saat idle

Gunakan ini saat server berjalan untuk publik.

---

### `builder\setuper.bat`
**Wajib dijalankan pertama kali.** Menyiapkan seluruh struktur folder dan file yang dibutuhkan server agar bisa berjalan.

---

### `builder\buildcore.bat`
Build plugin utama `ReminecraftCore` dari source (`core/`). Plugin ini menampilkan:
- **Bossbar** di atas layar: TPS server, jumlah pemain, penggunaan RAM (update tiap detik)
- **Action bar** per pemain: ping masing-masing pemain + TPS (update tiap detik)
- **Status check** di console setelah server startup

---

### `builder\buildnative.bat`
Build C++ native library (`reminecraft_native.dll`) menggunakan CMake + MSVC.  
Library ini menangani kompresi paket jaringan dengan Windows Compression API (MSZIP/deflate).  
Butuh CMake dan Visual Studio Build Tools terinstall di sistem.

---

### `builder\buildplugin.bat`
Build plugin pihak ketiga dari source. Penggunaan:

```bat
builder\buildplugin.bat authme
builder\buildplugin.bat fastlogin
builder\buildplugin.bat floodgate
builder\buildplugin.bat geyser
```

Hasil build otomatis di-copy ke `plugin\` dan `runfolder\plugins\`.

---

### `builder\buildserver.bat`
Build Purpur server JAR dari source (`source\java\`).  
Jarang dibutuhkan kecuali ada modifikasi di server core.

---

## Struktur Folder

```
reminecraft\
├── localhost.bat          ← Jalankan server (development)
├── runner.bat             ← Jalankan server (production)
├── config.json            ← Konfigurasi utama ReMinecraft
├── log4j2.xml             ← Konfigurasi logging
│
├── builder\               ← Semua script build
│   ├── setuper.bat        ← Setup pertama kali (jalankan ini dulu!)
│   ├── buildcore.bat      ← Build plugin ReminecraftCore
│   ├── buildnative.bat    ← Build C++ native DLL
│   ├── buildplugin.bat    ← Build plugin authme/fastlogin/geyser/floodgate
│   └── buildserver.bat    ← Build Purpur server dari source
│
├── core\                  ← Source code ReminecraftCore plugin (Maven)
│
├── plugin\                ← Pre-built JARs plugin (letakkan di sini)
│
├── runfolder\             ← Folder kerja server saat berjalan
│   ├── plugins\           ← Plugin aktif server
│   ├── config\            ← Config Paper (paper-global.yml, dll)
│   ├── purpur.yml
│   ├── spigot.yml
│   ├── bukkit.yml
│   └── server.properties
│
└── source\                ← Source code semua komponen
    ├── authme\            ← AuthMe plugin source
    ├── fastlogin\         ← FastLogin plugin source
    ├── geyser\            ← Geyser (Bedrock bridge) source
    ├── floodgate\         ← Floodgate source
    ├── native\            ← C++ JNI library source
    └── bun\               ← Bun/TypeScript sidecar scripting
```

---

## Konfigurasi Utama

Edit `config.json` untuk mengatur:
- IP dan port Java (`25565`) dan Bedrock (`19132`)
- Koneksi database (MySQL)
- Fitur native compression dan Bun scripting

Edit `runfolder\server.properties` untuk mengatur:
- `max-players` — batas pemain
- `online-mode` — `false` untuk server cracked
- `view-distance` / `simulation-distance` — performa chunk

---

## Plugin Bawaan

| Plugin | Fungsi |
|--------|--------|
| Geyser | Bridge Bedrock ↔ Java |
| Floodgate | Auth Bedrock player tanpa akun Java |
| AuthMe | Login/register sistem untuk server offline |
| FastLogin | Auto-login untuk pemain premium |
| LuckPerms | Manajemen permission/group |
| ProtocolLib | Library protocol untuk plugin lain |
| ReminecraftCore | HUD bossbar TPS/ping, status check startup |
