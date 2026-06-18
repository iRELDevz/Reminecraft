package org.reminecraft.auth;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

class AuthCommand implements CommandExecutor {

    private final AuthManager mgr;
    private final AuthStorage storage;

    AuthCommand(AuthManager mgr, AuthStorage storage) {
        this.mgr     = mgr;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();
        switch (name) {
            case "login"      -> handleLogin(sender, args);
            case "register"   -> handleRegister(sender, args);
            case "logout"     -> handleLogout(sender);
            case "changepass" -> handleChangepass(sender, args);
            case "auth"       -> handleAuth(sender, args);
        }
        return true;
    }

    private void handleLogin(CommandSender sender, String[] args) {
        Player p = asPlayer(sender); if (p == null) return;
        if (mgr.isAuthed(p.getUniqueId())) { msg(p, "Kamu sudah login.", NamedTextColor.YELLOW); return; }
        if (!mgr.isRegistered(p.getUniqueId())) { msg(p, "Belum terdaftar. Ketik /register <password> <confirm>", NamedTextColor.RED); return; }
        if (args.length < 1) { msg(p, "Gunakan: /login <password>", NamedTextColor.RED); return; }
        if (mgr.attempts(p.getUniqueId()) >= mgr.maxAttempts()) {
            p.kick(Component.text("Terlalu banyak percobaan login.", NamedTextColor.RED));
            return;
        }
        String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";
        if (mgr.login(p.getUniqueId(), p.getName(), args[0], ip)) {
            msg(p, "Login berhasil!", NamedTextColor.GREEN);
        } else {
            int left = mgr.maxAttempts() - mgr.attempts(p.getUniqueId());
            msg(p, "Password salah. Sisa percobaan: " + left, NamedTextColor.RED);
            if (left <= 0) p.kick(Component.text("Terlalu banyak percobaan login.", NamedTextColor.RED));
        }
    }

    private void handleRegister(CommandSender sender, String[] args) {
        Player p = asPlayer(sender); if (p == null) return;
        if (mgr.isAuthed(p.getUniqueId())) { msg(p, "Kamu sudah login.", NamedTextColor.YELLOW); return; }
        if (mgr.isRegistered(p.getUniqueId())) { msg(p, "Sudah terdaftar. Gunakan /login <password>", NamedTextColor.YELLOW); return; }
        if (args.length < 2) { msg(p, "Gunakan: /register <password> <confirm>", NamedTextColor.RED); return; }
        if (!args[0].equals(args[1])) { msg(p, "Password tidak cocok.", NamedTextColor.RED); return; }
        String err = mgr.validatePassword(args[0]);
        if (err != null) { msg(p, err, NamedTextColor.RED); return; }
        String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";
        if (mgr.register(p.getUniqueId(), p.getName(), args[0], ip)) {
            msg(p, "Registrasi berhasil! Selamat datang.", NamedTextColor.GREEN);
        } else {
            msg(p, "Registrasi gagal.", NamedTextColor.RED);
        }
    }

    private void handleLogout(CommandSender sender) {
        Player p = asPlayer(sender); if (p == null) return;
        if (!mgr.isAuthed(p.getUniqueId())) { msg(p, "Kamu belum login.", NamedTextColor.YELLOW); return; }
        mgr.logout(p.getUniqueId());
        msg(p, "Berhasil logout.", NamedTextColor.GREEN);
        String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";
        storage.touch(p.getUniqueId(), 0L, ip);
    }

    private void handleChangepass(CommandSender sender, String[] args) {
        Player p = asPlayer(sender); if (p == null) return;
        if (!mgr.isAuthed(p.getUniqueId())) { msg(p, "Login terlebih dahulu.", NamedTextColor.RED); return; }
        if (args.length < 3) { msg(p, "Gunakan: /changepass <lama> <baru> <confirm>", NamedTextColor.RED); return; }
        if (!args[1].equals(args[2])) { msg(p, "Password baru tidak cocok.", NamedTextColor.RED); return; }
        String err = mgr.validatePassword(args[1]);
        if (err != null) { msg(p, err, NamedTextColor.RED); return; }
        if (mgr.changePassword(p.getUniqueId(), args[0], args[1])) {
            msg(p, "Password berhasil diganti.", NamedTextColor.GREEN);
        } else {
            msg(p, "Password lama salah.", NamedTextColor.RED);
        }
    }

    private void handleAuth(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reminecraftauth.admin")) {
            sender.sendMessage(Component.text("Tidak ada izin.", NamedTextColor.RED)); return;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("/auth info|reload|forcelogin|unregister <player>", NamedTextColor.YELLOW)); return;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (sender instanceof Player p) p.sendMessage(Component.text("Reload tidak tersedia saat runtime.", NamedTextColor.YELLOW));
                else sender.sendMessage("Reload tidak tersedia saat runtime.");
            }
            case "forcelogin" -> {
                if (args.length < 2) { sender.sendMessage("Gunakan: /auth forcelogin <player>"); return; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player tidak ditemukan atau offline."); return; }
                mgr.markBedrock(target.getUniqueId());
                sender.sendMessage("Force-login ke " + target.getName() + " berhasil.");
                msg(target, "Kamu telah di-force-login oleh admin.", NamedTextColor.YELLOW);
            }
            case "unregister" -> {
                if (args.length < 2) { sender.sendMessage("Gunakan: /auth unregister <player>"); return; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target != null) {
                    mgr.unregister(target.getUniqueId());
                    sender.sendMessage("Unregister " + target.getName() + " berhasil.");
                } else {
                    sender.sendMessage("Player harus online untuk di-unregister.");
                }
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage("Gunakan: /auth info <player>"); return; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player tidak ditemukan atau offline."); return; }
                UUID uuid = target.getUniqueId();
                sender.sendMessage("[ReminecraftAuth] " + target.getName()
                    + " | authed=" + mgr.isAuthed(uuid)
                    + " | bedrock=" + mgr.isBedrock(uuid)
                    + " | registered=" + mgr.isRegistered(uuid)
                    + " | attempts=" + mgr.attempts(uuid));
            }
            default -> sender.sendMessage("Sub-command tidak dikenal: " + args[0]);
        }
    }

    private Player asPlayer(CommandSender s) {
        if (!(s instanceof Player p)) { s.sendMessage("Hanya bisa digunakan oleh player."); return null; }
        return p;
    }

    private void msg(Player p, String text, NamedTextColor color) {
        p.sendMessage(Component.text(text, color));
    }
}
