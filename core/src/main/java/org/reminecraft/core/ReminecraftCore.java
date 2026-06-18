package org.reminecraft.core;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class ReminecraftCore extends JavaPlugin implements Listener {

    private BossBar hudBar;
    private HttpClient httpClient;
    private final AtomicReference<WebSocket> bridge = new AtomicReference<>();
    private volatile boolean shuttingDown = false;

    @Override
    public void onEnable() {
        hudBar = BossBar.bossBar(
            Component.text("Reminecraft — Memuat..."),
            1.0f,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        );

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(hudBar);

        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);

        httpClient = HttpClient.newHttpClient();
        connectBridge();
    }

    // ── Bun Bridge ────────────────────────────────────────────

    private void connectBridge() {
        if (shuttingDown) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:25500/bridge"), new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket ws) {
                            bridge.set(ws);
                            ws.request(Long.MAX_VALUE);
                            getLogger().info("Connected to Bun sidecar (dashboard live).");
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                            bridge.set(null);
                            if (!shuttingDown) {
                                getLogger().info("Bun sidecar disconnected. Retrying in 5s...");
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                                connectBridge();
                            }
                            return null;
                        }

                        @Override
                        public void onError(WebSocket ws, Throwable err) {
                            bridge.set(null);
                            if (!shuttingDown) {
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                                connectBridge();
                            }
                        }
                    }).get();
            } catch (Exception ignored) {}
        });
    }

    private void sendToBun(String json) {
        WebSocket ws = bridge.get();
        if (ws == null || ws.isOutputClosed()) return;
        try { ws.sendText(json, true); } catch (Exception ignored) {}
    }

    // ── Tick (every 1 second) ─────────────────────────────────

    private void tick() {
        double tps   = Math.min(20.0, Bukkit.getTPS()[0]);
        int online   = Bukkit.getOnlinePlayers().size();
        int max      = Bukkit.getMaxPlayers();
        Runtime rt   = Runtime.getRuntime();
        long usedMB  = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long maxMB   = rt.maxMemory() / 1048576L;

        TextColor tpsColor;
        BossBar.Color barColor;
        if (tps >= 19.5) {
            tpsColor = NamedTextColor.GREEN;
            barColor = BossBar.Color.GREEN;
        } else if (tps >= 15.0) {
            tpsColor = NamedTextColor.YELLOW;
            barColor = BossBar.Color.YELLOW;
        } else {
            tpsColor = NamedTextColor.RED;
            barColor = BossBar.Color.RED;
        }

        // Bossbar
        Component title = Component.empty()
            .append(Component.text("TPS: ", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f", tps), tpsColor))
            .append(Component.text("  │  Players: ", NamedTextColor.GRAY))
            .append(Component.text(online + "/" + max, NamedTextColor.WHITE))
            .append(Component.text("  │  RAM: ", NamedTextColor.GRAY))
            .append(Component.text(usedMB + "MB/" + maxMB + "MB", NamedTextColor.AQUA));

        hudBar.name(title);
        hudBar.progress(Math.max(0.001f, (float)(tps / 20.0)));
        hudBar.color(barColor);

        // Action bar + collect player data
        int totalPing = 0;
        StringBuilder players = new StringBuilder("[");
        boolean first = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            int ping = p.getPing();
            totalPing += ping;

            TextColor pingColor = ping < 80  ? NamedTextColor.GREEN
                                : ping < 150 ? NamedTextColor.YELLOW
                                             : NamedTextColor.RED;

            p.sendActionBar(Component.empty()
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(ping + "ms", pingColor))
                .append(Component.text("  │  TPS: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", tps), tpsColor)));

            if (!first) players.append(",");
            players.append(String.format("{\"name\":\"%s\",\"ping\":%d}",
                p.getName().replace("\"", "\\\""), ping));
            first = false;
        }

        players.append("]");
        int avgPing = online > 0 ? totalPing / online : 0;

        // Send to Bun dashboard
        sendToBun(String.format(
            "{\"type\":\"stats\",\"tps\":%.2f,\"online\":%d,\"max_players\":%d," +
            "\"ram_used_mb\":%d,\"ram_max_mb\":%d,\"avg_ping\":%d,\"players\":%s}",
            tps, online, max, usedMB, maxMB, avgPing, players
        ));
    }

    // ── Events ────────────────────────────────────────────────

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        Bukkit.getScheduler().runTaskLater(this, this::printStatus, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().showBossBar(hudBar);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.getPlayer().hideBossBar(hudBar);
    }

    // ── Startup Status ────────────────────────────────────────

    private void printStatus() {
        Runtime rt   = Runtime.getRuntime();
        long usedMB  = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long maxMB   = rt.maxMemory() / 1048576L;
        double[] tps = Bukkit.getTPS();
        int view     = Bukkit.getViewDistance();
        int sim      = Bukkit.getSimulationDistance();
        int maxPl    = Bukkit.getMaxPlayers();
        Plugin[] all = Bukkit.getPluginManager().getPlugins();
        long active  = Arrays.stream(all).filter(Plugin::isEnabled).count();

        String sep = "==============================================";
        log(sep);
        log("  REMINECRAFT — SERVER STATUS CHECK");
        log(sep);
        log(String.format("  TPS    : %.2f (1m)  %.2f (5m)  %.2f (15m)", tps[0], tps[1], tps[2]));
        log(String.format("  Memory : %dMB used / %dMB max", usedMB, maxMB));
        log("  ──────────────────────────────────────────");
        log(String.format("  Max Players    : %d", maxPl));
        log(String.format("  View Distance  : %d chunks", view));
        log(String.format("  Sim Distance   : %d chunks", sim));
        log(String.format("  Online Mode    : %s", Bukkit.getServer().getOnlineMode()));
        log(String.format("  Plugins        : %d active / %d loaded", active, all.length));
        log("  ──────────────────────────────────────────");

        boolean ok = true;
        if (tps[0] < 19.0)         { warn(String.format("TPS rendah: %.2f", tps[0])); ok = false; }
        if (view > 7)               { warn("View distance " + view + " > 7");           ok = false; }
        if (sim > 5)                { warn("Sim distance " + sim + " > 5");             ok = false; }
        if (maxMB - usedMB < 512)  { warn("Sisa RAM hanya " + (maxMB-usedMB) + "MB"); ok = false; }

        if (ok) log("  Status : OK");
        log(sep);
    }

    private void log(String msg)  { getLogger().info(msg); }
    private void warn(String msg) { getLogger().warning("  [!] " + msg); }

    @Override
    public void onDisable() {
        shuttingDown = true;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(hudBar);
        WebSocket ws = bridge.get();
        if (ws != null) ws.abort();
        getLogger().info("ReminecraftCore disabled.");
    }
}
