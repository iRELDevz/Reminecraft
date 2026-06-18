package org.reminecraft.perms.api;

import org.bukkit.entity.Player;
import org.reminecraft.perms.model.Group;
import org.reminecraft.perms.storage.Storage;

import java.util.*;

public final class PermsAPI {

    private static Storage storage;

    public static void init(Storage s) { storage = s; }

    // ── Groups ────────────────────────────────────────────────

    public static Group getGroup(String name) {
        return storage.getGroup(name);
    }

    public static Collection<Group> getAllGroups() {
        return storage.getAllGroups();
    }

    public static void createGroup(String name, int weight, String prefix) {
        storage.putGroup(new Group(name, weight, prefix, new ArrayList<>()));
    }

    public static boolean deleteGroup(String name) {
        return storage.removeGroup(name);
    }

    // ── Players ───────────────────────────────────────────────

    public static Group getPlayerGroup(Player player) {
        return storage.getPlayerGroup(player.getUniqueId());
    }

    public static String getPlayerGroupName(Player player) {
        return storage.getPlayerGroupName(player.getUniqueId());
    }

    public static void setPlayerGroup(Player player, String groupName) {
        storage.setPlayerGroup(player.getUniqueId(), player.getName(), groupName);
    }

    public static List<String> getPlayerExtraPerms(Player player) {
        return storage.getPlayerExtra(player.getUniqueId());
    }

    public static void addPlayerPerm(Player player, String node) {
        List<String> extra = new ArrayList<>(storage.getPlayerExtra(player.getUniqueId()));
        if (!extra.contains(node)) {
            extra.add(node);
            storage.setPlayerExtra(player.getUniqueId(), player.getName(), extra);
        }
    }

    public static void removePlayerPerm(Player player, String node) {
        List<String> extra = new ArrayList<>(storage.getPlayerExtra(player.getUniqueId()));
        extra.remove(node);
        storage.setPlayerExtra(player.getUniqueId(), player.getName(), extra);
    }

    // ── Permissions ───────────────────────────────────────────

    public static Set<String> getAllPermissions(Player player) {
        Set<String> perms = new LinkedHashSet<>();
        Group group = storage.getPlayerGroup(player.getUniqueId());
        if (group != null) perms.addAll(group.getPermissions());
        perms.addAll(storage.getPlayerExtra(player.getUniqueId()));
        return perms;
    }

    public static boolean hasPermission(Player player, String node) {
        Set<String> perms = getAllPermissions(player);
        return perms.contains("*") || perms.contains(node);
    }

    public static void ensurePlayer(Player player) {
        storage.ensurePlayer(player.getUniqueId(), player.getName());
    }
}
