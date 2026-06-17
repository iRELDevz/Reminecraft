import { initializeDatabase } from "./database.ts";
import { loadAllScripts, startScriptWatcher } from "./script_loader.ts";

const PORT = 25500;
const clients = new Set<any>();

async function main() {
    console.log("====================================================");
    console.log("          REMINE-CRAFT BUN SIDECAR SERVER");
    console.log("====================================================");

    await initializeDatabase();
    await loadAllScripts();
    startScriptWatcher();

    Bun.serve({
        port: PORT,
        fetch(req, server) {
            const url = new URL(req.url);

            if (url.pathname === "/bridge") {
                if (server.upgrade(req)) {
                    return;
                }
                return new Response("Upgrade failed", { status: 400 });
            }

            if (url.pathname === "/api/status") {
                return new Response(JSON.stringify({
                    status: "online",
                    time: new Date().toISOString(),
                    uptime: process.uptime()
                }), {
                    headers: { "Content-Type": "application/json" }
                });
            }

            return new Response("Reminecraft Bun API - Page Not Found", { status: 404 });
        },
        websocket: {
            open(ws) {
                clients.add(ws);
                console.log("[ReMinecraft|BUN|] Java bridge connected.");
            },
            message(ws, message) {
                const MAX_MESSAGE_SIZE = 65536;
                if (message.length > MAX_MESSAGE_SIZE) {
                    console.error("[ReMinecraft|BUN|] Message too large, dropping.");
                    ws.close(1009, "Message too large");
                    return;
                }
                try {
                    const data = JSON.parse(message.toString());
                    console.log("[ReMinecraft|BUN|] Packet from Java:", data);

                    if (data.type === "event") {
                    }
                } catch (e) {
                    console.error("[ReMinecraft|BUN|] Invalid JSON packet:", message);
                }
            },
            close(ws, code, message) {
                clients.delete(ws);
                console.log("[ReMinecraft|BUN|] Java bridge disconnected.");
            }
        }
    });

    console.log(`[ReMinecraft|BUN|] HTTP & WebSocket running on port ${PORT}`);
}

main().catch(error => {
    console.error("[ReMinecraft|BUN|] Fatal startup error:", error);
});
