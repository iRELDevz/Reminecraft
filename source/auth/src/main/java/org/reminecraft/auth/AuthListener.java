package org.reminecraft.auth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

class AuthListener implements Listener {

    private final ReminecraftAuth plugin;
    private final AuthManager mgr;
    private final boolean allowMovement;
    private final boolean kickOnWrong;
    private final int timeoutSecs;

    AuthListener(ReminecraftAuth plugin, AuthManager mgr) {
        this.plugin       = plugin;
        this.mgr          = mgr;
        this.allowMovement = plugin.getConfig().getBoolean("allow-movement", false);
        this.kickOnWrong  = plugin.getConfig().getBoolean("kick-on-wrong-password", false);
        this.timeoutSecs  = plugin.getConfig().getInt("login-timeout-seconds", 30);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";

        String prefix = plugin.getConfig().getString("floodgate-prefix", ".");
        if (name.startsWith(prefix)) {
            mgr.markBedrock(uuid);
            return;
        }

        mgr.markJoined(uuid);

        if (mgr.trySession(uuid, ip)) {
            p.sendMessage(Component.text("Selamat datang kembali, " + name + "!", NamedTextColor.GREEN));
            return;
        }

        boolean registered = mgr.isRegistered(uuid);
        sendAuthPrompt(p, registered);
        startTimeoutTask(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onQuit(PlayerQuitEvent e) {
        mgr.cleanup(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onChat(AsyncChatEvent e) {
        if (!mgr.isAuthed(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onCommand(PlayerCommandPreprocessEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (mgr.isAuthed(uuid)) return;
        String cmd = e.getMessage().toLowerCase();
        if (!cmd.startsWith("/login") && !cmd.startsWith("/l ")
                && !cmd.startsWith("/register") && !cmd.startsWith("/reg ")) {
            e.setCancelled(true);
            sendAuthPrompt(e.getPlayer(), mgr.isRegistered(uuid));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onMove(PlayerMoveEvent e) {
        if (allowMovement) return;
        UUID uuid = e.getPlayer().getUniqueId();
        if (mgr.isAuthed(uuid)) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to != null && (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onDrop(PlayerDropItemEvent e) {
        if (!mgr.isAuthed(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!mgr.isAuthed(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onInteract(PlayerInteractEvent e) {
        if (!mgr.isAuthed(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    private void sendAuthPrompt(Player p, boolean registered) {
        if (registered) {
            p.sendMessage(Component.text("Ketik ", NamedTextColor.YELLOW)
                .append(Component.text("/login <password>", NamedTextColor.WHITE))
                .append(Component.text(" untuk masuk.", NamedTextColor.YELLOW)));
        } else {
            p.sendMessage(Component.text("Ketik ", NamedTextColor.YELLOW)
                .append(Component.text("/register <password> <confirm>", NamedTextColor.WHITE))
                .append(Component.text(" untuk daftar.", NamedTextColor.YELLOW)));
        }
    }

    private void startTimeoutTask(Player p) {
        UUID uuid = p.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                if (mgr.isAuthed(uuid)) return;
                p.kick(Component.text("Waktu login habis. Silakan masuk kembali.", NamedTextColor.RED));
            }
        }.runTaskLater(plugin, timeoutSecs * 20L);
    }
}
