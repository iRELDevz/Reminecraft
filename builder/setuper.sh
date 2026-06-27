#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="[ReMinecraft|SETUP|]"
ERR="[ReMinecraft|ERROR|]"

echo "===================================================="
echo "             REMINE-CRAFT SETUP"
echo "===================================================="

JDK_DIR="$ROOT/jdk-25"
MVN_DIR="$ROOT/apache-maven-3.9.16"
MVN="$MVN_DIR/bin/mvn"

# ── 1. JDK ──────────────────────────────────────────────
if [ ! -x "$JDK_DIR/bin/java" ]; then
    echo "$LOG JDK 25 tidak ditemukan. Mengunduh dari Eclipse Adoptium..."
    JDK_TAR="$(mktemp /tmp/jdk25.XXXXXX.tar.gz)"
    curl -fsSL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$JDK_TAR"
    mkdir -p "$ROOT/jdk-tmp"
    tar -xzf "$JDK_TAR" -C "$ROOT/jdk-tmp"
    mv "$ROOT/jdk-tmp"/jdk-25* "$JDK_DIR"
    rm -rf "$ROOT/jdk-tmp" "$JDK_TAR"
    echo "$LOG JDK 25 siap."
else
    echo "$LOG JDK 25 OK."
fi

export JAVA_HOME="$JDK_DIR"
JAVA_EXE="$JDK_DIR/bin/java"

# ── 2. Maven ─────────────────────────────────────────────
if [ ! -f "$MVN" ]; then
    echo "$LOG Maven tidak ditemukan. Mengunduh..."
    MVN_ZIP="$(mktemp /tmp/maven.XXXXXX.zip)"
    curl -fsSL "https://downloads.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz" -o "$MVN_ZIP"
    tar -xzf "$MVN_ZIP" -C "$ROOT"
    rm -f "$MVN_ZIP"
    echo "$LOG Maven siap."
else
    echo "$LOG Maven OK."
fi

# ── 3. Server JAR ────────────────────────────────────────
if [ ! -f "$ROOT/runfolder/reminecraft-server.jar" ]; then
    echo "$LOG Server JAR tidak ditemukan. Mengunduh Purpur..."
    mkdir -p "$ROOT/runfolder"
    curl -fsSL "https://api.purpurmc.org/v2/purpur/26.1.2/latest/download" \
        -o "$ROOT/runfolder/reminecraft-server.jar"
    echo "$LOG Server JAR siap."
else
    echo "$LOG Server JAR OK."
fi

# ── 4. Folders ───────────────────────────────────────────
mkdir -p "$ROOT/runfolder/plugins" "$ROOT/scripts"

# ── 5. Build plugins ─────────────────────────────────────
build() {
    local name="$1" srcdir="$2" outjar="$3"
    echo "$LOG Build $name..."
    cd "$srcdir"
    "$MVN" package -DskipTests -q
    local jar
    jar=$(find "$srcdir/target" -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" ! -name "original-*.jar" | head -1)
    cp -f "$jar" "$ROOT/runfolder/plugins/$outjar"
    echo "$LOG $name -> plugins/$outjar"
}

build "ReminecraftCore"    "$ROOT/core"             "reminecraft-core.jar"
build "ReminecraftPerms"   "$ROOT/source/perms"     "reminecraft-perms.jar"
build "ReminecraftAuth"    "$ROOT/source/auth"      "reminecraft-auth.jar"
build "ReminecraftDevmode" "$ROOT/source/devmode"   "reminecraft-devmode.jar"
build "ReminecraftGPU"     "$ROOT/source/gpu"       "reminecraft-gpu.jar"

# ── 6. EULA ──────────────────────────────────────────────
printf "#https://aka.ms/MinecraftEULA\neula=true\n" > "$ROOT/runfolder/eula.txt"

# ── 7. server.properties ─────────────────────────────────
if [ ! -f "$ROOT/runfolder/server.properties" ]; then
    cat > "$ROOT/runfolder/server.properties" <<'PROPS'
server-ip=0.0.0.0
server-port=25565
max-players=100
online-mode=false
enable-command-block=true
spawn-protection=0
view-distance=10
simulation-distance=8
level-name=world
level-type=minecraft\:normal
gamemode=survival
difficulty=normal
allow-nether=true
pvp=true
white-list=false
enforce-whitelist=false
motd=Reminecraft - Java + Bedrock
PROPS
    echo "$LOG server.properties dibuat."
fi

cd "$ROOT"
echo ""
echo "===================================================="
echo "$LOG Setup selesai!"
echo "$LOG Jalankan: bash localhost.sh"
echo "===================================================="
