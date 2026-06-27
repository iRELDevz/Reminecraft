#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"

JAR="$DIR/reminecraft-gpu.jar"
[ -f "$JAR" ] || JAR="$DIR/runfolder/plugins/reminecraft-gpu.jar"
[ -f "$JAR" ] || { echo "[ReMinecraft|GPU|] reminecraft-gpu.jar tidak ditemukan. Copy jar plugin ke folder ini."; exit 1; }

PORT="${1:-25599}"
BIND="${2:-0.0.0.0}"

echo "[ReMinecraft|GPU|] Starting daemon on $BIND:$PORT ..."
echo "[ReMinecraft|GPU|] Jalankan lewat tunnel privat (Tailscale/WireGuard), jangan expose ke publik."
exec java --enable-native-access=ALL-UNNAMED -cp "$JAR" org.reminecraft.gpu.daemon.GpuDaemon "$PORT" "$BIND"
