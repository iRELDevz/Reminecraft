package org.reminecraft.devmode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class RateLimiter implements Listener {

    private final ReminecraftDevmode plugin;
    private volatile Map<String, Long> cooldowns = Map.of();
    private final Map<UUID, Map<String, Long>> lastUsed = new ConcurrentHashMap<>();

    RateLimiter(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("rate-limiter.cooldowns");
        if (sec == null) { cooldowns = Map.of(); return; }

        Map<String, Long> map = new HashMap<>();
        for (String key : sec.getKeys(false))
            map.put(key.toLowerCase(), sec.getLong(key) * 1000L);
        cooldowns = Map.copyOf(map);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onCommand(PlayerCommandPreprocessEvent e) {
        Map<String, Long> snap = cooldowns;
        if (snap.isEmpty()) return;

        String raw = e.getMessage().substring(1);
        String cmd = raw.split(" ")[0].toLowerCase();
        if (cmd.contains(":")) cmd = cmd.substring(cmd.indexOf(':') + 1);

        Long cd = snap.get(cmd);
        if (cd == null) return;

        UUID uuid = e.getPlayer().getUniqueId();
        Map<String, Long> used = lastUsed.computeIfAbsent(uuid, k -> new HashMap<>());

        long now  = System.currentTimeMillis();
        long last = used.getOrDefault(cmd, 0L);
        long left = cd - (now - last);

        if (left > 0) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text(
                "Tunggu " + (left / 1000 + 1) + "s sebelum pakai /" + cmd + " lagi.",
                NamedTextColor.RED
            ));
            return;
        }
        used.put(cmd, now);
    }

    @EventHandler
    void onQuit(PlayerQuitEvent e) {
        lastUsed.remove(e.getPlayer().getUniqueId());
    }
}
