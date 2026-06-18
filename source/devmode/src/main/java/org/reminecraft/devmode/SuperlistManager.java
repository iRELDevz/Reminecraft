package org.reminecraft.devmode;

import org.bukkit.Bukkit;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class SuperlistManager {

    private final ReminecraftDevmode plugin;
    private final Set<UUID> members = ConcurrentHashMap.newKeySet();

    SuperlistManager(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        load();
    }

    void load() {
        members.clear();
        for (String s : plugin.getConfig().getStringList("superlist")) {
            try { members.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
        syncOps();
    }

    boolean isMember(UUID uuid) { return members.contains(uuid); }

    void add(UUID uuid) {
        if (members.add(uuid)) {
            Bukkit.getOfflinePlayer(uuid).setOp(true);
            save();
        }
    }

    void remove(UUID uuid) {
        if (members.remove(uuid)) {
            Bukkit.getOfflinePlayer(uuid).setOp(false);
            save();
        }
    }

    Set<UUID> members() { return members; }

    private void syncOps() {
        for (UUID uuid : members)
            Bukkit.getOfflinePlayer(uuid).setOp(true);
    }

    private void save() {
        List<String> list = members.stream().map(UUID::toString).toList();
        plugin.getConfig().set("superlist", list);
        plugin.saveConfig();
    }
}
