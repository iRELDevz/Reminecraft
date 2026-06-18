package org.reminecraft.devmode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.StringJoiner;

class DevmodeCommand implements CommandExecutor {

    private final ReminecraftDevmode plugin;
    private final AntiBotManager antiBot;
    private final ChatFilter chatFilter;
    private final RateLimiter rateLimiter;
    private final GroupMessages groupMessages;

    DevmodeCommand(ReminecraftDevmode plugin, AntiBotManager antiBot,
                   ChatFilter chatFilter, RateLimiter rateLimiter, GroupMessages groupMessages) {
        this.plugin       = plugin;
        this.antiBot      = antiBot;
        this.chatFilter   = chatFilter;
        this.rateLimiter  = rateLimiter;
        this.groupMessages = groupMessages;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("reminecraftdevmode.admin")) {
            s.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) { usage(s); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage("§aReminecraftDevmode reloaded.");
            }
            case "plugin" -> handlePlugin(s, Arrays.copyOfRange(args, 1, args.length));
            case "filter" -> handleFilter(s, Arrays.copyOfRange(args, 1, args.length));
            case "unban"  -> {
                if (args.length < 2) { s.sendMessage("§cUsage: /devmode unban <ip>"); return true; }
                antiBot.unban(args[1]);
                s.sendMessage("§aIP di-unban: " + args[1]);
            }
            default -> usage(s);
        }
        return true;
    }

    private void handlePlugin(CommandSender s, String[] args) {
        if (args.length == 0) {
            s.sendMessage("§cUsage: /devmode plugin <list|enable|disable|load|reload> [nama]");
            return;
        }
        var pm = plugin.getServer().getPluginManager();

        switch (args[0].toLowerCase()) {
            case "list" -> {
                StringJoiner sj = new StringJoiner("§7, ");
                for (Plugin p : pm.getPlugins())
                    sj.add((p.isEnabled() ? "§a" : "§c") + p.getName());
                s.sendMessage("§fPlugins (" + pm.getPlugins().length + "): " + sj);
            }
            case "enable" -> {
                if (args.length < 2) { s.sendMessage("§cNama plugin?"); return; }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage("§cPlugin tidak ditemukan."); return; }
                if (p.isEnabled()) { s.sendMessage("§ePlugin sudah aktif."); return; }
                pm.enablePlugin(p);
                s.sendMessage("§aEnabled: " + p.getName());
            }
            case "disable" -> {
                if (args.length < 2) { s.sendMessage("§cNama plugin?"); return; }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage("§cPlugin tidak ditemukan."); return; }
                if (p == plugin) { s.sendMessage("§cTidak bisa disable diri sendiri."); return; }
                if (!p.isEnabled()) { s.sendMessage("§ePlugin sudah tidak aktif."); return; }
                pm.disablePlugin(p);
                s.sendMessage("§cDisabled: " + p.getName());
            }
            case "load" -> {
                if (args.length < 2) { s.sendMessage("§cNama file JAR?"); return; }
                File pluginsDir = plugin.getServer().getUpdateFolderFile().getParentFile();
                File f = new File(pluginsDir, args[1].endsWith(".jar") ? args[1] : args[1] + ".jar");
                if (!f.exists()) { s.sendMessage("§cFile tidak ditemukan: " + f.getName()); return; }
                try {
                    Plugin loaded = pm.loadPlugin(f);
                    if (loaded == null) { s.sendMessage("§cGagal load plugin."); return; }
                    pm.enablePlugin(loaded);
                    s.sendMessage("§aLoaded & enabled: " + loaded.getName());
                } catch (Exception ex) {
                    s.sendMessage("§cError: " + ex.getMessage());
                }
            }
            case "reload" -> {
                if (args.length < 2) { s.sendMessage("§cNama plugin?"); return; }
                if (args[1].equalsIgnoreCase(plugin.getName())) {
                    plugin.reload();
                    s.sendMessage("§aDevmode reloaded.");
                    return;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage("§cPlugin tidak ditemukan."); return; }
                pm.disablePlugin(p);
                pm.enablePlugin(p);
                s.sendMessage("§aReloaded: " + p.getName());
            }
            default -> s.sendMessage("§cSub-command tidak dikenal: " + args[0]);
        }
    }

    private void handleFilter(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage("§cUsage: /devmode filter <add|remove|list> [pattern]");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add" -> {
                chatFilter.addPattern(args[1]);
                s.sendMessage("§aFilter ditambah: " + args[1]);
            }
            case "remove" -> {
                chatFilter.removePattern(args[1]);
                s.sendMessage("§aFilter dihapus: " + args[1]);
            }
            case "list" -> {
                var list = chatFilter.getPatterns();
                if (list.isEmpty()) { s.sendMessage("§7Tidak ada filter aktif."); return; }
                list.forEach(p -> s.sendMessage("§7- " + p.pattern()));
            }
            default -> s.sendMessage("§cSub-command tidak dikenal: " + args[0]);
        }
    }

    private void usage(CommandSender s) {
        s.sendMessage("§b/devmode reload");
        s.sendMessage("§b/devmode plugin <list|enable|disable|load|reload> [nama]");
        s.sendMessage("§b/devmode filter <add|remove|list> [pattern]");
        s.sendMessage("§b/devmode unban <ip>");
    }
}
