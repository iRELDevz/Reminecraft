package org.reminecraft.core;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class ReminecraftCore extends JavaPlugin implements Listener {

    // Unique invisible entries per scoreboard line (using §0–§9 as fake player names)
    private static final String[] SLOTS = {
        "§0","§1","§2","§3","§4","§5","§6","§7","§8"
    };

    private BossBar hudBar;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    private HttpClient httpClient;
    private final AtomicReference<WebSocket> bridge = new AtomicReference<>();
    private volatile boolean shuttingDown = false;

    @Override
    public void onEnable() {
        hudBar = BossBar.bossBar(
            Component.text("Reminecraft"),
            1.0f,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        );

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(hudBar);
            assignBoard(p);
        }

        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);

        httpClient = HttpClient.newHttpClient();
        connectBridge();
    }

    // ── Scoreboard ────────────────────────────────────────────

    private Scoreboard buildBoard() {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("hud", Criteria.DUMMY,
            legacy("§6§lReMinecraft"));

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        try {
            obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable ignored) {}

        for (int i = 0; i < SLOTS.length; i++) {
            Team t = board.registerNewTeam("rl" + i);
            t.addEntry(SLOTS[i]);
            obj.getScore(SLOTS[i]).setScore(SLOTS.length - i);
        }
        return board;
    }

    private void assignBoard(Player p) {
        Scoreboard board = buildBoard();
        boards.put(p.getUniqueId(), board);
        p.setScoreboard(board);
    }

    private void updateBoard(Player p, double tps, int online, int max, long usedMB, long maxMB) {
        Scoreboard board = boards.get(p.getUniqueId());
        if (board == null) return;

        int ping = p.getPing();

        String tc = tps >= 19.5 ? "§a" : tps >= 15 ? "§e" : "§c";
        String pc = ping < 80   ? "§a" : ping < 150  ? "§e" : "§c";

        double ramPct = maxMB > 0 ? (usedMB * 100.0 / maxMB) : 0;
        String rc = ramPct < 70 ? "§a" : ramPct < 85 ? "§e" : "§c";

        // 9 lines, index 0 = top slot
        String[] lines = {
            "§8 ─────────────────",
            " §7TPS   " + tc + String.format("%.1f", tps),
            " §7Ping  " + pc + ping + " ms",
            " §7MSPT  §f" + String.format("%.1f", Math.max(0, 50 - (tps / 20.0 * 50))) + " ms",
            "§8 ─────────────────",
            " §7Online §b" + online + " §7/ §f" + max,
            " §7RAM   " + rc + usedMB + "§7/" + maxMB + " MB",
            "§8 ─────────────────",
            " §8reminecraft.net",
        };

        for (int i = 0; i < lines.length && i < SLOTS.length; i++) {
            Team t = board.getTeam("rl" + i);
            if (t != null) t.prefix(legacy(lines[i]));
        }
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }

    // ── Tick (every second) ───────────────────────────────────

    private void tick() {
        double tps  = Math.min(20.0, Bukkit.getTPS()[0]);
        int online  = Bukkit.getOnlinePlayers().size();
        int max     = Bukkit.getMaxPlayers();
        Runtime rt  = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long maxMB  = rt.maxMemory() / 1048576L;

        TextColor tpsColor = tps >= 19.5 ? NamedTextColor.GREEN
                           : tps >= 15.0 ? NamedTextColor.YELLOW
                           : NamedTextColor.RED;
        BossBar.Color barColor = tps >= 19.5 ? BossBar.Color.GREEN
                               : tps >= 15.0 ? BossBar.Color.YELLOW
                               : BossBar.Color.RED;

        // Bossbar (top)
        hudBar.name(Component.empty()
            .append(Component.text("TPS: ", NamedTextColor.GRAY))
            .append(Component.text(String.format("%.1f", tps), tpsColor))
            .append(Component.text("  │  Players: ", NamedTextColor.GRAY))
            .append(Component.text(online + "/" + max, NamedTextColor.WHITE))
            .append(Component.text("  │  RAM: ", NamedTextColor.GRAY))
            .append(Component.text(usedMB + "MB/" + maxMB + "MB", NamedTextColor.AQUA)));
        hudBar.progress(Math.max(0.001f, (float)(tps / 20.0)));
        hudBar.color(barColor);

        // Per-player: action bar + scoreboard sidebar
        int totalPing = 0;
        StringBuilder players = new StringBuilder("[");
        boolean first = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            int ping = p.getPing();
            totalPing += ping;

            TextColor pingColor = ping < 80  ? NamedTextColor.GREEN
                                : ping < 150 ? NamedTextColor.YELLOW
                                             : NamedTextColor.RED;

            // Action bar (above hotbar)
            p.sendActionBar(Component.empty()
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(ping + "ms", pingColor))
                .append(Component.text("  │  TPS: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.1f", tps), tpsColor)));

            // Scoreboard sidebar (right side)
            updateBoard(p, tps, online, max, usedMB, maxMB);

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
            tps, online, max, usedMB, maxMB, avgPing, players));
    }

    // ── Events ────────────────────────────────────────────────

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        Bukkit.getScheduler().runTaskLater(this, this::printStatus, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        p.showBossBar(hudBar);
        assignBoard(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        p.hideBossBar(hudBar);
        boards.remove(p.getUniqueId());
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
                            getLogger().info("Connected to Bun sidecar.");
                        }
                        @Override
                        public CompletionStage<?> onText(WebSocket ws, CharSequence d, boolean l) {
                            return null;
                        }
                        @Override
                        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                            bridge.set(null);
                            if (!shuttingDown) {
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                                connectBridge();
                            }
                            return null;
                        }
                        @Override
                        public void onError(WebSocket ws, Throwable e) {
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
        if (tps[0] < 19.0)        { warn(String.format("TPS rendah: %.2f", tps[0])); ok = false; }
        if (view > 7)              { warn("View distance " + view + " > 7");           ok = false; }
        if (sim > 5)               { warn("Sim distance " + sim + " > 5");             ok = false; }
        if (maxMB - usedMB < 512) { warn("Sisa RAM hanya " + (maxMB - usedMB) + "MB"); ok = false; }
        if (ok) log("  Status : OK");
        log(sep);
    }

    private void log(String msg)  { getLogger().info(msg); }
    private void warn(String msg) { getLogger().warning("  [!] " + msg); }

    @Override
    public void onDisable() {
        shuttingDown = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(hudBar);
        }
        boards.clear();
        WebSocket ws = bridge.get();
        if (ws != null) ws.abort();
        getLogger().info("ReminecraftCore disabled.");
    }
}
