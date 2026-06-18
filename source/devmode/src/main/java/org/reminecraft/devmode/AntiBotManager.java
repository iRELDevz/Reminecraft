package org.reminecraft.devmode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class AntiBotManager implements Listener {

    private final ReminecraftDevmode plugin;
    private final Map<String, List<Long>> joinTimes = new ConcurrentHashMap<>();
    private final Set<String> banned = ConcurrentHashMap.newKeySet();

    private int maxJoins;
    private long windowMs;
    private long banMs;

    AntiBotManager(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanup, 6000L, 6000L);
    }

    void reload() {
        var cfg = plugin.getConfig();
        maxJoins = cfg.getInt("anti-bot.max-joins", 5);
        windowMs = cfg.getLong("anti-bot.window-seconds", 60) * 1000L;
        banMs    = cfg.getLong("anti-bot.ban-seconds", 120) * 1000L;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onLogin(PlayerLoginEvent e) {
        String ip = e.getAddress().getHostAddress();

        if (banned.contains(ip)) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                Component.text("Terlalu banyak koneksi. Coba lagi nanti.", NamedTextColor.RED));
            return;
        }

        long now = System.currentTimeMillis();
        List<Long> times = joinTimes.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (times) {
            times.removeIf(t -> now - t > windowMs);
            times.add(now);

            if (times.size() >= maxJoins) {
                banned.add(ip);
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(
                    plugin, () -> banned.remove(ip), banMs / 50);
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    Component.text("Flood terdeteksi. Coba lagi dalam " + (banMs / 1000) + " detik.", NamedTextColor.RED));
            }
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        joinTimes.entrySet().removeIf(entry -> {
            List<Long> times = entry.getValue();
            synchronized (times) {
                times.removeIf(t -> now - t > windowMs);
                return times.isEmpty();
            }
        });
    }

    void unban(String ip) { banned.remove(ip); }
    Set<String> banned()  { return banned; }
}
