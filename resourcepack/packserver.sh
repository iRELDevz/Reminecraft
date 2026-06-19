#!/usr/bin/env bash
PACK_FILE="$(cd "$(dirname "$0")" && pwd)/reminecraft-java.zip"
PORT=8080

if [ ! -f "$PACK_FILE" ]; then
    echo "[ReMinecraft|PACKSERVER|] reminecraft-java.zip not found, exiting."
    exit 1
fi

python3 - "$PACK_FILE" "$PORT" <<'EOF'
import sys, http.server, socketserver

pack_file = sys.argv[1]
port = int(sys.argv[2])

class PackHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        with open(pack_file, "rb") as f:
            data = f.read()
        self.send_response(200)
        self.send_header("Content-Type", "application/zip")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "public, max-age=3600")
        self.end_headers()
        self.wfile.write(data)
    def log_message(self, *a):
        pass

with socketserver.TCPServer(("", port), PackHandler) as httpd:
    httpd.serve_forever()
EOF
