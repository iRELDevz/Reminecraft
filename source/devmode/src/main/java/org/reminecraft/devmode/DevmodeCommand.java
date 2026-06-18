package org.reminecraft.devmode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        this.plugin        = plugin;
        this.antiBot       = antiBot;
        this.chatFilter    = chatFilter;
        this.rateLimiter   = rateLimiter;
        this.groupMessages = groupMessages;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("reminecraftdevmode.admin")) {
            s.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { usage(s); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                s.sendMessage(Component.text("ReminecraftDevmode reloaded.", NamedTextColor.GREEN));
            }
            case "plugin" -> handlePlugin(s, Arrays.copyOfRange(args, 1, args.length));
            case "filter" -> handleFilter(s, Arrays.copyOfRange(args, 1, args.length));
            case "unban"  -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /devmode unban <ip>", NamedTextColor.RED)); return true; }
                antiBot.unban(args[1]);
                s.sendMessage(Component.text("IP di-unban: " + args[1], NamedTextColor.GREEN));
            }
            default -> usage(s);
        }
        return true;
    }

    private void handlePlugin(CommandSender s, String[] args) {
        if (args.length == 0) {
            s.sendMessage(Component.text("Usage: /devmode plugin <list|enable|disable|load|reload> [nama]", NamedTextColor.RED));
            return;
        }
        var pm = plugin.getServer().getPluginManager();

        switch (args[0].toLowerCase()) {
            case "list" -> {
                StringJoiner sj = new StringJoiner(", ");
                for (Plugin p : pm.getPlugins())
                    sj.add((p.isEnabled() ? "§a" : "§c") + p.getName());
                s.sendMessage(Component.text("Plugins (" + pm.getPlugins().length + "): " + sj, NamedTextColor.WHITE));
            }
            case "enable" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Nama plugin?", NamedTextColor.RED)); return; }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage(Component.text("Plugin tidak ditemukan.", NamedTextColor.RED)); return; }
                if (p.isEnabled()) { s.sendMessage(Component.text("Plugin sudah aktif.", NamedTextColor.YELLOW)); return; }
                pm.enablePlugin(p);
                s.sendMessage(Component.text("Enabled: " + p.getName(), NamedTextColor.GREEN));
            }
            case "disable" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Nama plugin?", NamedTextColor.RED)); return; }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage(Component.text("Plugin tidak ditemukan.", NamedTextColor.RED)); return; }
                if (p == plugin) { s.sendMessage(Component.text("Tidak bisa disable diri sendiri.", NamedTextColor.RED)); return; }
                if (!p.isEnabled()) { s.sendMessage(Component.text("Plugin sudah tidak aktif.", NamedTextColor.YELLOW)); return; }
                pm.disablePlugin(p);
                s.sendMessage(Component.text("Disabled: " + p.getName(), NamedTextColor.RED));
            }
            case "load" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Nama file JAR?", NamedTextColor.RED)); return; }
                File pluginsDir = plugin.getServer().getUpdateFolderFile().getParentFile();
                File f = new File(pluginsDir, args[1].endsWith(".jar") ? args[1] : args[1] + ".jar");
                if (!f.exists()) { s.sendMessage(Component.text("File tidak ditemukan: " + f.getName(), NamedTextColor.RED)); return; }
                try {
                    Plugin loaded = pm.loadPlugin(f);
                    if (loaded == null) { s.sendMessage(Component.text("Gagal load plugin.", NamedTextColor.RED)); return; }
                    pm.enablePlugin(loaded);
                    s.sendMessage(Component.text("Loaded & enabled: " + loaded.getName(), NamedTextColor.GREEN));
                } catch (Exception ex) {
                    s.sendMessage(Component.text("Error: " + ex.getMessage(), NamedTextColor.RED));
                }
            }
            case "reload" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Nama plugin?", NamedTextColor.RED)); return; }
                if (args[1].equalsIgnoreCase(plugin.getName())) {
                    plugin.reload();
                    s.sendMessage(Component.text("Devmode reloaded.", NamedTextColor.GREEN));
                    return;
                }
                Plugin p = pm.getPlugin(args[1]);
                if (p == null) { s.sendMessage(Component.text("Plugin tidak ditemukan.", NamedTextColor.RED)); return; }
                pm.disablePlugin(p);
                pm.enablePlugin(p);
                s.sendMessage(Component.text("Reloaded: " + p.getName(), NamedTextColor.GREEN));
            }
            default -> s.sendMessage(Component.text("Sub-command tidak dikenal: " + args[0], NamedTextColor.RED));
        }
    }

    private void handleFilter(CommandSender s, String[] args) {
        if (args.length == 0) {
            s.sendMessage(Component.text("Usage: /devmode filter <add|remove|list> [pattern]", NamedTextColor.RED));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /devmode filter add <pattern>", NamedTextColor.RED)); return; }
                chatFilter.addPattern(args[1]);
                s.sendMessage(Component.text("Filter ditambah: " + args[1], NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /devmode filter remove <pattern>", NamedTextColor.RED)); return; }
                chatFilter.removePattern(args[1]);
                s.sendMessage(Component.text("Filter dihapus: " + args[1], NamedTextColor.GREEN));
            }
            case "list" -> {
                var list = chatFilter.getPatterns();
                if (list.isEmpty()) { s.sendMessage(Component.text("Tidak ada filter aktif.", NamedTextColor.GRAY)); return; }
                list.forEach(p -> s.sendMessage(Component.text("- " + p.pattern(), NamedTextColor.GRAY)));
            }
            default -> s.sendMessage(Component.text("Sub-command tidak dikenal: " + args[0], NamedTextColor.RED));
        }
    }

    private void usage(CommandSender s) {
        s.sendMessage(Component.text("/devmode reload", NamedTextColor.AQUA));
        s.sendMessage(Component.text("/devmode plugin <list|enable|disable|load|reload> [nama]", NamedTextColor.AQUA));
        s.sendMessage(Component.text("/devmode filter <add|remove|list> [pattern]", NamedTextColor.AQUA));
        s.sendMessage(Component.text("/devmode unban <ip>", NamedTextColor.AQUA));
    }
}
