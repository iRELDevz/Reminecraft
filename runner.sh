#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_INIT="[ReMinecraft|INIT|]"
LOG_JAVA="[ReMinecraft|JAVA|]"
LOG_ERROR="[ReMinecraft|ERROR|]"

echo "===================================================="
echo "          REMINE-CRAFT PRODUCTION SERVER"
echo "===================================================="

if [ ! -f "$ROOT/runfolder/reminecraft-server.jar" ]; then
    echo "$LOG_ERROR Server JAR tidak ditemukan. Jalankan builder/setuper.sh dulu."
    exit 1
fi

JAVA_EXE="java"
source "$ROOT/_javadetect.sh"

mkdir -p "$ROOT/runfolder/plugins"
if [ -d "$ROOT/plugin" ]; then
    for f in "$ROOT/plugin"/*.jar; do
        [ -e "$f" ] || continue
        cp -f "$f" "$ROOT/runfolder/plugins/"
        echo "$LOG_INIT Plugin: $(basename "$f")"
    done
fi

printf "#https://aka.ms/MinecraftEULA\neula=true\n" > "$ROOT/runfolder/eula.txt"

if [ -f "$ROOT/resourcepack/reminecraft-java.zip" ]; then
    bash "$ROOT/resourcepack/packserver.sh" &
    PACKSERVER_PID=$!
    trap 'kill $PACKSERVER_PID 2>/dev/null || true' EXIT
fi

echo "$LOG_JAVA Starting production (4G RAM, G1GC)..."
cd "$ROOT/runfolder"
"$JAVA_EXE" \
    -Xms2G -Xmx4G \
    -XX:+UseG1GC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:G1NewSizePercent=30 \
    -XX:G1MaxNewSizePercent=40 \
    -XX:G1ReservePercent=15 \
    -XX:G1HeapRegionSize=32m \
    -XX:InitiatingHeapOccupancyPercent=15 \
    -XX:G1MixedGCLiveThresholdPercent=90 \
    -XX:G1HeapWastePercent=5 \
    -XX:G1MixedGCCountTarget=4 \
    -XX:G1OldCSetRegionThresholdPercent=5 \
    -XX:SurvivorRatio=32 \
    -XX:MaxTenuringThreshold=1 \
    -Dterminal.jline=false \
    -Dterminal.ansi=true \
    -jar reminecraft-server.jar nogui
