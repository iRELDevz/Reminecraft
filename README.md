<div align="center">

# ReMinecraft

**Hybrid Java + Bedrock Minecraft Server**

![Version](https://img.shields.io/badge/version-1.1.0-success) ![Java + Bedrock](https://img.shields.io/badge/Java%20%2B%20Bedrock-Purpur%201.21-blue) ![GPU](https://img.shields.io/badge/GPU-OpenCL-orange)

[![English](#-english) · [Indonesia](#-indonesia) · [日本語](#-日本語)](#)

</div>

---

## 🇬🇧 English

A hybrid Minecraft server supporting **Java Edition** and **Bedrock Edition** players simultaneously, built on Purpur with Geyser + Floodgate.

### Quick Start

```
1. git clone https://github.com/iRELDevz/Reminecraft
2. builder\setuper.bat   ← run once, downloads everything
3. localhost.bat         ← start the server
```

That's it. The setup script handles JDK, Maven, and the server JAR automatically.

### What `setuper.bat` Does

| Step | What happens |
|------|-------------|
| JDK 25 | Downloads from Eclipse Adoptium if not found |
| Apache Maven | Downloads if not found |
| Server JAR | Downloads Purpur 26.1.2 if not found |
| Custom plugins | Builds ReminecraftCore + ReminecraftPerms from source |

All third-party plugins (`AuthMe`, `FastLogin`, `Geyser`, `Floodgate`, `ProtocolLib`) are already included in the repo at `runfolder/plugins/` — no manual downloading needed.

### Running the Server

| Script | Use case |
|--------|----------|
| `localhost.bat` | Local development (ZGC, 4 GB max heap) |
| `runner.bat` | Production (G1GC, tuned for many players) |

### Rebuild Plugins

Only needed if you modify source code:

| Script | Rebuilds |
|--------|----------|
| `builder\buildcore.bat` | ReminecraftCore (HUD, status check) |
| `builder\buildperms.bat` | ReminecraftPerms (permission/rank system) |
| `builder\buildplugin.bat` | AuthMe / FastLogin / Geyser / Floodgate from source |
| `builder\buildserver.bat` | Purpur server JAR from source (takes 10–40 min) |

### Bundled Plugins

| Plugin | Purpose |
|--------|---------|
| Geyser | Bedrock ↔ Java bridge |
| Floodgate | Auth for Bedrock players |
| AuthMe | Login/register for offline-mode servers |
| FastLogin | Auto-login for premium Java players |
| ProtocolLib | Packet-level library used by other plugins |
| ReminecraftCore | In-game HUD (TPS / ping / RAM / scoreboard) |
| ReminecraftPerms | Custom rank and permission system |

### Folder Structure

```
reminecraft\
├── localhost.bat           ← Start (development)
├── runner.bat              ← Start (production)
├── config.json             ← Main config
├── scripts\                ← Bun scripting files (.ts)
├── builder\
│   ├── setuper.bat         ← First-time setup (run this)
│   ├── buildcore.bat
│   ├── buildperms.bat
│   ├── buildplugin.bat
│   └── buildserver.bat
├── core\                   ← ReminecraftCore source (Maven)
├── source\
│   ├── perms\              ← ReminecraftPerms source (Maven)
│   └── bun\                ← Bun/TypeScript sidecar
└── runfolder\              ← Server working directory
    ├── plugins\            ← All plugin JARs (tracked in git)
    ├── config\
    ├── purpur.yml
    ├── spigot.yml
    └── server.properties
```

---

## 🇮🇩 Indonesia

Server Minecraft hybrid yang mendukung pemain **Java Edition** dan **Bedrock Edition** secara bersamaan, berbasis Purpur dengan Geyser + Floodgate.

### Quick Start

```
1. git clone https://github.com/iRELDevz/Reminecraft
2. builder\setuper.bat   ← jalankan sekali, download otomatis
3. localhost.bat         ← jalankan server
```

Selesai. Setup script mengurus JDK, Maven, dan server JAR secara otomatis.

### Yang Dilakukan `setuper.bat`

| Langkah | Keterangan |
|---------|-----------|
| JDK 25 | Download dari Eclipse Adoptium jika belum ada |
| Apache Maven | Download jika belum ada |
| Server JAR | Download Purpur 26.1.2 jika belum ada |
| Plugin custom | Build ReminecraftCore + ReminecraftPerms dari source |

Semua plugin pihak ketiga (`AuthMe`, `FastLogin`, `Geyser`, `Floodgate`, `ProtocolLib`) sudah ada di repo di `runfolder/plugins/` — tidak perlu download manual.

### Menjalankan Server

| Script | Kegunaan |
|--------|----------|
| `localhost.bat` | Development lokal (ZGC, maks 4 GB heap) |
| `runner.bat` | Produksi (G1GC, dioptimasi untuk banyak pemain) |

### Rebuild Plugin

Hanya perlu jika mengubah source code:

| Script | Yang di-rebuild |
|--------|----------------|
| `builder\buildcore.bat` | ReminecraftCore (HUD, status check) |
| `builder\buildperms.bat` | ReminecraftPerms (sistem permission/rank) |
| `builder\buildplugin.bat` | AuthMe / FastLogin / Geyser / Floodgate dari source |
| `builder\buildserver.bat` | Purpur server JAR dari source (10–40 menit) |

### Plugin Bawaan

| Plugin | Fungsi |
|--------|--------|
| Geyser | Bridge Bedrock ↔ Java |
| Floodgate | Auth untuk pemain Bedrock |
| AuthMe | Sistem login/register untuk server offline |
| FastLogin | Auto-login untuk pemain Java premium |
| ProtocolLib | Library packet untuk plugin lain |
| ReminecraftCore | HUD in-game (TPS / ping / RAM / scoreboard) |
| ReminecraftPerms | Sistem rank dan permission custom |

### Struktur Folder

```
reminecraft\
├── localhost.bat           ← Jalankan (development)
├── runner.bat              ← Jalankan (produksi)
├── config.json             ← Config utama
├── scripts\                ← File scripting Bun (.ts)
├── builder\
│   ├── setuper.bat         ← Setup pertama kali (jalankan ini)
│   ├── buildcore.bat
│   ├── buildperms.bat
│   ├── buildplugin.bat
│   └── buildserver.bat
├── core\                   ← Source ReminecraftCore (Maven)
├── source\
│   ├── perms\              ← Source ReminecraftPerms (Maven)
│   └── bun\                ← Sidecar Bun/TypeScript
└── runfolder\              ← Folder kerja server
    ├── plugins\            ← Semua plugin JAR (ada di repo)
    ├── config\
    ├── purpur.yml
    ├── spigot.yml
    └── server.properties
```

---

## 🇯🇵 日本語

**JavaエディションとBedrockエディション**の両方のプレイヤーに対応したハイブリッドMinecraftサーバーです。PurpurをベースにGeyser + Floodgateを使用しています。

### クイックスタート

```
1. git clone https://github.com/iRELDevz/Reminecraft
2. builder\setuper.bat   ← 一度だけ実行、自動でダウンロード
3. localhost.bat         ← サーバー起動
```

セットアップスクリプトがJDK・Maven・サーバーJARを自動で処理します。

### `setuper.bat` の処理内容

| ステップ | 内容 |
|---------|------|
| JDK 25 | 見つからない場合はEclipse Adoptiumからダウンロード |
| Apache Maven | 見つからない場合はダウンロード |
| サーバーJAR | 見つからない場合はPurpur 26.1.2をダウンロード |
| カスタムプラグイン | ReminecraftCore + ReminecraftPermsをソースからビルド |

サードパーティプラグイン（`AuthMe`・`FastLogin`・`Geyser`・`Floodgate`・`ProtocolLib`）はすでにリポジトリの `runfolder/plugins/` に含まれています — 手動ダウンロード不要。

### サーバーの起動

| スクリプト | 用途 |
|-----------|------|
| `localhost.bat` | ローカル開発用（ZGC、最大4 GB） |
| `runner.bat` | 本番用（G1GC、多人数向け最適化） |

### プラグインの再ビルド

ソースコードを変更した場合のみ必要：

| スクリプト | ビルド対象 |
|-----------|-----------|
| `builder\buildcore.bat` | ReminecraftCore（HUD、ステータスチェック） |
| `builder\buildperms.bat` | ReminecraftPerms（権限・ランクシステム） |
| `builder\buildplugin.bat` | AuthMe / FastLogin / Geyser / Floodgateをソースから |
| `builder\buildserver.bat` | PurpurサーバーJARをソースから（10〜40分） |

### 同梱プラグイン

| プラグイン | 機能 |
|-----------|------|
| Geyser | Bedrock ↔ Java ブリッジ |
| Floodgate | Bedrockプレイヤーの認証 |
| AuthMe | オフラインモード用ログイン/登録システム |
| FastLogin | Javaプレミアムプレイヤーの自動ログイン |
| ProtocolLib | 他プラグイン向けパケットライブラリ |
| ReminecraftCore | ゲーム内HUD（TPS / ping / RAM / スコアボード） |
| ReminecraftPerms | カスタムランク・権限システム |

### フォルダ構成

```
reminecraft\
├── localhost.bat           ← 起動（開発用）
├── runner.bat              ← 起動（本番用）
├── config.json             ← メイン設定
├── scripts\                ← Bunスクリプトファイル（.ts）
├── builder\
│   ├── setuper.bat         ← 初回セットアップ（これを実行）
│   ├── buildcore.bat
│   ├── buildperms.bat
│   ├── buildplugin.bat
│   └── buildserver.bat
├── core\                   ← ReminecraftCoreソース（Maven）
├── source\
│   ├── perms\              ← ReminecraftPermsソース（Maven）
│   └── bun\                ← Bun/TypeScriptサイドカー
└── runfolder\              ← サーバー作業ディレクトリ
    ├── plugins\            ← 全プラグインJAR（リポジトリ内）
    ├── config\
    ├── purpur.yml
    ├── spigot.yml
    └── server.properties
```
