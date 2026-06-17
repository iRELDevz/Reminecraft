<div align="center">

# ReMinecraft

**Hybrid Java + Bedrock Minecraft Server**

[![English](#-english) · [Indonesia](#-indonesia) · [日本語](#-日本語)](#)

</div>

---

## 🇬🇧 English

A hybrid Minecraft server supporting **Java Edition** and **Bedrock Edition** players simultaneously, built on Purpur (Paper fork) with Geyser + Floodgate.

### Requirements

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java (JDK) | 21+ | Required. JDK 25 recommended at `jdk-25/` |
| Apache Maven | 3.9+ | Bundled at `apache-maven-3.9.16/` or install manually |
| CMake | 3.20+ | For building the C++ native DLL |
| Visual Studio Build Tools | 2019+ | MSVC compiler for CMake (Windows) |
| Bun | Latest | For sidecar scripting (`source/bun/`) |

### First-Time Setup

Follow these steps in order:

**1. Get the server JAR**  
Place `reminecraft-server.jar` (Purpur) in the root folder. Download from [purpurmc.org](https://purpurmc.org).

**2. Get plugin JARs**  
Place the following JARs in the `plugin\` folder:
```
plugin\AuthMe.jar
plugin\FastLogin.jar
plugin\floodgate-spigot.jar
plugin\Geyser-Spigot.jar
plugin\LuckPerms.jar
plugin\ProtocolLib.jar
```

**3. Run setup**
```bat
builder\setuper.bat
```

**4. Build the core plugin**
```bat
builder\buildcore.bat
```

**5. (Optional) Build native DLL**
```bat
builder\buildnative.bat
```

**6. Start the server**
- Development: `localhost.bat`
- Production: `runner.bat`

### Scripts

| Script | Description |
|--------|-------------|
| `localhost.bat` | Start server for local development (ZGC, 4GB RAM) |
| `runner.bat` | Start server for production (G1GC, optimized for many players) |
| `builder\setuper.bat` | **Run first.** Creates all folders, copies plugins, generates default configs |
| `builder\buildcore.bat` | Build the ReminecraftCore plugin (bossbar HUD, startup status) |
| `builder\buildnative.bat` | Build the C++ native compression DLL (requires CMake + MSVC) |
| `builder\buildplugin.bat` | Build third-party plugins from source (`authme`, `fastlogin`, `geyser`, `floodgate`) |
| `builder\buildserver.bat` | Build the Purpur server JAR from source |

### Bundled Plugins

| Plugin | Purpose |
|--------|---------|
| Geyser | Bedrock ↔ Java bridge |
| Floodgate | Auth for Bedrock players without Java accounts |
| AuthMe | Login/register system for offline-mode servers |
| FastLogin | Auto-login for premium players |
| LuckPerms | Permission and group management |
| ProtocolLib | Protocol library for other plugins |
| ReminecraftCore | Live bossbar HUD (TPS/ping/RAM) + startup status check |

### Folder Structure

```
reminecraft\
├── localhost.bat          ← Start server (development)
├── runner.bat             ← Start server (production)
├── config.json            ← Main ReMinecraft config
├── builder\               ← All build scripts
│   ├── setuper.bat        ← First-time setup (run this first!)
│   ├── buildcore.bat      ← Build ReminecraftCore plugin
│   ├── buildnative.bat    ← Build C++ native DLL
│   ├── buildplugin.bat    ← Build authme/fastlogin/geyser/floodgate
│   └── buildserver.bat    ← Build Purpur server from source
├── core\                  ← ReminecraftCore plugin source (Maven)
├── plugin\                ← Place pre-built plugin JARs here
├── runfolder\             ← Server working directory
│   ├── plugins\           ← Active server plugins
│   ├── config\            ← Paper configs (paper-global.yml, etc.)
│   ├── purpur.yml
│   ├── spigot.yml
│   ├── bukkit.yml
│   └── server.properties
└── source\                ← All component source code
    ├── authme\
    ├── fastlogin\
    ├── geyser\
    ├── floodgate\
    ├── native\            ← C++ JNI library
    └── bun\               ← Bun/TypeScript sidecar
```

---

## 🇮🇩 Indonesia

Server Minecraft hybrid yang mendukung pemain **Java Edition** dan **Bedrock Edition** secara bersamaan, berbasis Purpur (Paper fork) dengan Geyser + Floodgate.

### Persyaratan

| Kebutuhan | Versi | Keterangan |
|-----------|-------|------------|
| Java (JDK) | 21+ | Wajib. Disarankan JDK 25 di `jdk-25/` |
| Apache Maven | 3.9+ | Di `apache-maven-3.9.16/` atau install manual |
| CMake | 3.20+ | Untuk build C++ native DLL |
| Visual Studio Build Tools | 2019+ | MSVC compiler untuk CMake (Windows) |
| Bun | Latest | Untuk sidecar scripting (`source/bun/`) |

### Setup Pertama Kali

Ikuti langkah ini secara berurutan:

**1. Siapkan server JAR**  
Letakkan `reminecraft-server.jar` (Purpur) di folder root. Download dari [purpurmc.org](https://purpurmc.org).

**2. Siapkan plugin JARs**  
Letakkan file berikut di folder `plugin\`:
```
plugin\AuthMe.jar
plugin\FastLogin.jar
plugin\floodgate-spigot.jar
plugin\Geyser-Spigot.jar
plugin\LuckPerms.jar
plugin\ProtocolLib.jar
```

**3. Jalankan setup**
```bat
builder\setuper.bat
```

**4. Build plugin core**
```bat
builder\buildcore.bat
```

**5. (Opsional) Build native DLL**
```bat
builder\buildnative.bat
```

**6. Jalankan server**
- Development: `localhost.bat`
- Production: `runner.bat`

### Penjelasan Script

| Script | Fungsi |
|--------|--------|
| `localhost.bat` | Jalankan server untuk testing lokal (ZGC, 4GB RAM) |
| `runner.bat` | Jalankan server untuk production (G1GC, dioptimasi untuk banyak pemain) |
| `builder\setuper.bat` | **Jalankan pertama kali.** Buat folder, salin plugin, buat config default |
| `builder\buildcore.bat` | Build plugin ReminecraftCore (HUD bossbar, status startup) |
| `builder\buildnative.bat` | Build C++ native DLL untuk kompresi (butuh CMake + MSVC) |
| `builder\buildplugin.bat` | Build plugin dari source (`authme`, `fastlogin`, `geyser`, `floodgate`) |
| `builder\buildserver.bat` | Build Purpur server JAR dari source |

### Plugin Bawaan

| Plugin | Fungsi |
|--------|--------|
| Geyser | Bridge Bedrock ↔ Java |
| Floodgate | Auth pemain Bedrock tanpa akun Java |
| AuthMe | Sistem login/register untuk server offline |
| FastLogin | Auto-login untuk pemain premium |
| LuckPerms | Manajemen permission dan group |
| ProtocolLib | Library protocol untuk plugin lain |
| ReminecraftCore | HUD bossbar live (TPS/ping/RAM) + status check saat startup |

### Struktur Folder

```
reminecraft\
├── localhost.bat          ← Jalankan server (development)
├── runner.bat             ← Jalankan server (production)
├── config.json            ← Konfigurasi utama ReMinecraft
├── builder\               ← Semua script build
│   ├── setuper.bat        ← Setup pertama kali (jalankan ini dulu!)
│   ├── buildcore.bat      ← Build plugin ReminecraftCore
│   ├── buildnative.bat    ← Build C++ native DLL
│   ├── buildplugin.bat    ← Build authme/fastlogin/geyser/floodgate
│   └── buildserver.bat    ← Build Purpur server dari source
├── core\                  ← Source code ReminecraftCore plugin (Maven)
├── plugin\                ← Letakkan pre-built plugin JARs di sini
├── runfolder\             ← Folder kerja server saat berjalan
│   ├── plugins\           ← Plugin aktif server
│   ├── config\            ← Config Paper
│   ├── purpur.yml
│   ├── spigot.yml
│   ├── bukkit.yml
│   └── server.properties
└── source\                ← Source code semua komponen
    ├── authme\
    ├── fastlogin\
    ├── geyser\
    ├── floodgate\
    ├── native\            ← C++ JNI library
    └── bun\               ← Bun/TypeScript sidecar
```

---

## 🇯🇵 日本語

**JavaエディションとBedrockエディション**の両方のプレイヤーに対応したハイブリッドMinecraftサーバーです。Purpur（Paperフォーク）をベースに、GeyserとFloodgateを使用しています。

### 動作要件

| 必要環境 | バージョン | 備考 |
|---------|-----------|------|
| Java (JDK) | 21以上 | 必須。`jdk-25/` にJDK 25を推奨 |
| Apache Maven | 3.9以上 | `apache-maven-3.9.16/` に同梱、または手動インストール |
| CMake | 3.20以上 | C++ネイティブDLLのビルドに必要 |
| Visual Studio Build Tools | 2019以上 | CMake用MSVCコンパイラ（Windows） |
| Bun | 最新版 | サイドカースクリプト用（`source/bun/`） |

### 初回セットアップ

以下の手順を順番に実行してください：

**1. サーバーJARを準備する**  
`reminecraft-server.jar`（Purpur）をルートフォルダに配置します。[purpurmc.org](https://purpurmc.org) からダウンロードしてください。

**2. プラグインJARを準備する**  
以下のJARファイルを `plugin\` フォルダに配置します：
```
plugin\AuthMe.jar
plugin\FastLogin.jar
plugin\floodgate-spigot.jar
plugin\Geyser-Spigot.jar
plugin\LuckPerms.jar
plugin\ProtocolLib.jar
```

**3. セットアップを実行する**
```bat
builder\setuper.bat
```

**4. コアプラグインをビルドする**
```bat
builder\buildcore.bat
```

**5. （任意）ネイティブDLLをビルドする**
```bat
builder\buildnative.bat
```

**6. サーバーを起動する**
- 開発用: `localhost.bat`
- 本番用: `runner.bat`

### スクリプト説明

| スクリプト | 説明 |
|-----------|------|
| `localhost.bat` | ローカル開発用サーバー起動（ZGC、4GB RAM） |
| `runner.bat` | 本番用サーバー起動（G1GC、多人数向け最適化） |
| `builder\setuper.bat` | **最初に実行。** フォルダ作成・プラグインコピー・デフォルト設定生成 |
| `builder\buildcore.bat` | ReminecraftCoreプラグインをビルド（ボスバーHUD、起動時ステータス） |
| `builder\buildnative.bat` | C++ネイティブ圧縮DLLをビルド（CMake + MSVC 必須） |
| `builder\buildplugin.bat` | サードパーティプラグインをソースからビルド（`authme`, `fastlogin`, `geyser`, `floodgate`） |
| `builder\buildserver.bat` | ソースからPurpurサーバーJARをビルド |

### 同梱プラグイン

| プラグイン | 機能 |
|-----------|------|
| Geyser | Bedrock ↔ Java ブリッジ |
| Floodgate | Javaアカウントなしのbedrockプレイヤー認証 |
| AuthMe | オフラインモードサーバー用ログイン/登録システム |
| FastLogin | プレミアムプレイヤーの自動ログイン |
| LuckPerms | 権限・グループ管理 |
| ProtocolLib | 他プラグイン向けプロトコルライブラリ |
| ReminecraftCore | リアルタイムボスバーHUD（TPS/ping/RAM）+ 起動時ステータスチェック |

### フォルダ構成

```
reminecraft\
├── localhost.bat          ← サーバー起動（開発用）
├── runner.bat             ← サーバー起動（本番用）
├── config.json            ← ReMinecraft メイン設定
├── builder\               ← 全ビルドスクリプト
│   ├── setuper.bat        ← 初回セットアップ（最初に実行！）
│   ├── buildcore.bat      ← ReminecraftCoreプラグインをビルド
│   ├── buildnative.bat    ← C++ネイティブDLLをビルド
│   ├── buildplugin.bat    ← authme/fastlogin/geyser/floodgateをビルド
│   └── buildserver.bat    ← ソースからPurpurサーバーをビルド
├── core\                  ← ReminecraftCoreプラグインのソース（Maven）
├── plugin\                ← ビルド済みプラグインJARを配置する場所
├── runfolder\             ← サーバー動作ディレクトリ
│   ├── plugins\           ← 有効なサーバープラグイン
│   ├── config\            ← Paperの設定ファイル
│   ├── purpur.yml
│   ├── spigot.yml
│   ├── bukkit.yml
│   └── server.properties
└── source\                ← 全コンポーネントのソースコード
    ├── authme\
    ├── fastlogin\
    ├── geyser\
    ├── floodgate\
    ├── native\            ← C++ JNIライブラリ
    └── bun\               ← Bun/TypeScriptサイドカー
```
