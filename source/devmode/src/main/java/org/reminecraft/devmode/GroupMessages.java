package org.reminecraft.devmode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

class GroupMessages implements Listener {

    private final ReminecraftDevmode plugin;

    private volatile String defaultJoin;
    private volatile String defaultQuit;
    private volatile List<GroupEntry> groups = List.of();

    private record GroupEntry(String permission, String join, String quit) {}

    GroupMessages(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        var cfg = plugin.getConfig();
        defaultJoin = cfg.getString("group-messages.format.join", "&7{player} joined");
        defaultQuit = cfg.getString("group-messages.format.quit", "&7{player} left");

        List<GroupEntry> list = new ArrayList<>();
        ConfigurationSection sec = cfg.getConfigurationSection("group-messages.groups");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String perm = sec.getString(key + ".permission", "");
                String join = sec.getString(key + ".join", defaultJoin);
                String quit = sec.getString(key + ".quit", defaultQuit);
                list.add(new GroupEntry(perm, join, quit));
            }
        }
        groups = List.copyOf(list);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onJoin(PlayerJoinEvent e) {
        GroupEntry g = resolve(e.getPlayer());
        e.joinMessage(fmt(g != null ? g.join() : defaultJoin, e.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onQuit(PlayerQuitEvent e) {
        GroupEntry g = resolve(e.getPlayer());
        e.quitMessage(fmt(g != null ? g.quit() : defaultQuit, e.getPlayer().getName()));
    }

    private GroupEntry resolve(Player p) {
        for (GroupEntry g : groups) {
            if (!g.permission().isEmpty() && p.hasPermission(g.permission())) return g;
        }
        return null;
    }

    private Component fmt(String template, String name) {
        return LegacyComponentSerializer.legacyAmpersand()
            .deserialize(template.replace("{player}", name));
    }
}
