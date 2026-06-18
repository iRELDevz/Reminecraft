package org.reminecraft.perms.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.reminecraft.perms.ReminecraftPerms;
import org.reminecraft.perms.api.PermsAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final ReminecraftPerms plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PlayerListener(ReminecraftPerms plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PermsAPI.ensurePlayer(player);
        applyPermissions(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        revokePermissions(event.getPlayer());
    }

    public void applyPermissions(Player player) {
        revokePermissions(player);

        PermissionAttachment att = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), att);

        Set<String> perms = PermsAPI.getAllPermissions(player);

        if (perms.contains("*")) {
            Bukkit.getPluginManager().getPermissions()
                .forEach(p -> att.setPermission(p.getName(), true));
        } else {
            for (String node : perms) {
                if (node.startsWith("-")) {
                    att.setPermission(node.substring(1), false);
                } else {
                    att.setPermission(node, true);
                }
            }
        }

        player.recalculatePermissions();
    }

    public void revokePermissions(Player player) {
        PermissionAttachment att = attachments.remove(player.getUniqueId());
        if (att != null) {
            try { player.removeAttachment(att); } catch (Exception ignored) {}
        }
    }
}
