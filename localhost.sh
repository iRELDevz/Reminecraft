#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_INIT="[ReMinecraft|INIT|]"
LOG_JAVA="[ReMinecraft|JAVA|]"
LOG_ERROR="[ReMinecraft|ERROR|]"

echo "===================================================="
echo "          REMINE-CRAFT LOCALHOST SERVER"
echo "===================================================="

if [ ! -f "$ROOT/runfolder/reminecraft-server.jar" ]; then
    echo "$LOG_ERROR Server JAR tidak ditemukan. Jalankan builder/setuper.sh dulu."
    exit 1
fi

JAVA_EXE="java"
source "$ROOT/_javadetect.sh"

printf "#https://aka.ms/MinecraftEULA\neula=true\n" > "$ROOT/runfolder/eula.txt"

rm -f "$ROOT/runfolder/plugins/AuthMe.jar" \
      "$ROOT/runfolder/plugins/FastLogin.jar" \
      "$ROOT/runfolder/plugins/ProtocolLib.jar" \
      2>/dev/null || true

bash "$ROOT/resourcepack/packserver.sh" &
PACKSERVER_PID=$!
trap 'kill $PACKSERVER_PID 2>/dev/null || true' EXIT

echo "$LOG_JAVA Starting localhost (2G RAM)..."
cd "$ROOT/runfolder"
"$JAVA_EXE" \
    -Xms256M -Xmx2G \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:ReservedCodeCacheSize=64m \
    -XX:MaxMetaspaceSize=256m \
    -Dreminecraft.localhost=true \
    -jar reminecraft-server.jar nogui
