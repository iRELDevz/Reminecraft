import { initializeDatabase } from "./database.ts";
import { loadAllScripts, startScriptWatcher, setupScriptingGlobals } from "./script_loader.ts";
import { updateStats, getStats, getTpsHistory } from "./stats.ts";
import { emit } from "./scripting/events.ts";
import fs from "fs";
import path from "path";

const PORT = 25500;

// UI clients (browser dashboard connections)
const uiClients = new Set<any>();

// Java bridge connection (only one expected)
let javaConnection: any = null;

function broadcastToUI(data: object): void {
    const msg = JSON.stringify(data);
    for (const ws of uiClients) {
        try { ws.send(msg); } catch (_) {}
    }
}

function loadDashboard(): string {
    const htmlPath = path.resolve(import.meta.dir, "ui/dashboard.html");
    return fs.readFileSync(htmlPath, "utf-8");
}

async function main() {
    console.log("====================================================");
    console.log("          REMINE-CRAFT BUN SIDECAR SERVER");
    console.log("====================================================");

    await initializeDatabase();
    setupScriptingGlobals();
    await loadAllScripts();
    startScriptWatcher();

    Bun.serve({
        port: PORT,

        fetch(req, server) {
            const url = new URL(req.url);

            if (url.pathname === "/bridge") {
                if (server.upgrade(req, { data: { type: "bridge" } })) return;
                return new Response("Upgrade failed", { status: 400 });
            }

            if (url.pathname === "/ui") {
                if (server.upgrade(req, { data: { type: "ui" } })) return;
                return new Response("Upgrade failed", { status: 400 });
            }

            if (url.pathname === "/" || url.pathname === "/dashboard") {
                return new Response(loadDashboard(), {
                    headers: { "Content-Type": "text/html; charset=utf-8" },
                });
            }

            if (url.pathname === "/api/stats") {
                return new Response(
                    JSON.stringify({ ...getStats(), tps_history: getTpsHistory() }),
                    { headers: { "Content-Type": "application/json" } }
                );
            }

            if (url.pathname === "/api/status") {
                return new Response(
                    JSON.stringify({ status: "online", uptime: process.uptime(), java_connected: javaConnection !== null }),
                    { headers: { "Content-Type": "application/json" } }
                );
            }

            return new Response("Not Found", { status: 404 });
        },

        websocket: {
            open(ws) {
                const type = (ws.data as any)?.type;

                if (type === "bridge") {
                    javaConnection = ws;
                    console.log("[ReMinecraft|BUN|] Java bridge connected.");
                    broadcastToUI({ type: "java_connected" });
                } else if (type === "ui") {
                    uiClients.add(ws);
                    ws.send(JSON.stringify({
                        type: "init",
                        stats: getStats(),
                        tps_history: getTpsHistory(),
                    }));
                }
            },

            message(ws, message) {
                const type = (ws.data as any)?.type;

                if (type !== "bridge") return;

                if (typeof message !== "string" && !(message instanceof Buffer)) return;
                const raw = message.toString();

                if (raw.length > 65536) {
                    console.error("[ReMinecraft|BUN|] Message too large, dropping.");
                    ws.close(1009, "Message too large");
                    return;
                }

                let packet: any;
                try {
                    packet = JSON.parse(raw);
                } catch {
                    console.error("[ReMinecraft|BUN|] Invalid JSON from Java bridge.");
                    return;
                }

                if (packet.type === "stats") {
                    updateStats({
                        tps: packet.tps ?? 0,
                        online: packet.online ?? 0,
                        max_players: packet.max_players ?? 0,
                        ram_used_mb: packet.ram_used_mb ?? 0,
                        ram_max_mb: packet.ram_max_mb ?? 0,
                        avg_ping: packet.avg_ping ?? 0,
                        players: packet.players ?? [],
                    });

                    const stats = getStats();

                    emit("stats", stats);
                    if (stats.tps < 18) emit("tps_warning", stats);

                    broadcastToUI({
                        type: "stats",
                        ...stats,
                        tps_history: getTpsHistory(),
                    });
                }

                if (packet.type === "event") {
                    emit(packet.event, packet.data);
                }
            },

            close(ws) {
                const type = (ws.data as any)?.type;

                if (type === "bridge") {
                    javaConnection = null;
                    console.log("[ReMinecraft|BUN|] Java bridge disconnected.");
                    broadcastToUI({ type: "java_disconnected" });
                } else if (type === "ui") {
                    uiClients.delete(ws);
                }
            },
        },
    });

    console.log(`[ReMinecraft|BUN|] HTTP  → http://localhost:${PORT}/`);
    console.log(`[ReMinecraft|BUN|] WS UI → ws://localhost:${PORT}/ui`);
    console.log(`[ReMinecraft|BUN|] WS Bri→ ws://localhost:${PORT}/bridge`);
}

main().catch(err => {
    console.error("[ReMinecraft|BUN|] Fatal error:", err);
    process.exit(1);
});
