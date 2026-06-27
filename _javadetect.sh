#!/usr/bin/env bash

LOG_INIT="[ReMinecraft|INIT|]"
LOG_ERROR="[ReMinecraft|ERROR|]"

JAVA_EXE="java"

find_java() {
    local candidate

    for candidate in \
        "$ROOT/jdk-25/bin/java" \
        "$HOME/reminecraft/jdk-25/bin/java" \
        /usr/lib/jvm/java-25/bin/java \
        /usr/lib/jvm/jdk-25/bin/java \
        /usr/lib/jvm/temurin-25/bin/java
    do
        if [ -x "$candidate" ]; then
            JAVA_EXE="$candidate"
            return 0
        fi
    done

    for dir in \
        /usr/lib/jvm/jdk-2* \
        /usr/lib/jvm/java-2* \
        /usr/lib/jvm/temurin-2* \
        /opt/java/2*
    do
        if [ -x "$dir/bin/java" ]; then
            JAVA_EXE="$dir/bin/java"
            return 0
        fi
    done

    return 1
}

if command -v java >/dev/null 2>&1; then
    JAVA_MAJOR="$(
        java -version 2>&1 |
        awk -F'"' '/version/ {print $2}' |
        cut -d. -f1
    )"

    if [ "${JAVA_MAJOR:-0}" -ge 25 ] 2>/dev/null; then
        JAVA_EXE="$(command -v java)"
        return 0 2>/dev/null || exit 0
    fi

    echo "$LOG_INIT PATH Java is $JAVA_MAJOR, needs 25+. Searching..."
fi

if find_java; then
    return 0 2>/dev/null || exit 0
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
    return 0 2>/dev/null || exit 0
fi

echo "$LOG_ERROR Java 25+ not found. Install JDK 25 or run builder/setuper.sh."
exit 1
