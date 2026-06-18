package org.reminecraft.devmode;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;

class MotdListener implements Listener {

    private final ReminecraftDevmode plugin;
    private volatile boolean enabled;
    private volatile List<String> lines;

    MotdListener(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        var cfg = plugin.getConfig();
        enabled = cfg.getBoolean("motd.enabled", true);
        lines   = List.copyOf(cfg.getStringList("motd.lines"));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onPing(ServerListPingEvent e) {
        if (!enabled || lines.isEmpty()) return;

        double tps = Math.min(20.0, plugin.getServer().getTPS()[0]);
        String tpsStr = String.format("%.1f", tps);
        int online = plugin.getServer().getOnlinePlayers().size();
        int max    = plugin.getServer().getMaxPlayers();

        String first  = apply(lines.get(0), tpsStr, online, max);
        String second = lines.size() > 1 ? apply(lines.get(1), tpsStr, online, max) : "";

        String raw = second.isEmpty() ? first : first + "\n" + second;
        e.motd(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    private String apply(String line, String tps, int online, int max) {
        return line
            .replace("{tps}", tps)
            .replace("{online}", String.valueOf(online))
            .replace("{max}", String.valueOf(max));
    }
}
