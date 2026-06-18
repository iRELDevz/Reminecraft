package org.reminecraft.perms.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.reminecraft.perms.ReminecraftPerms;
import org.reminecraft.perms.api.PermsAPI;
import org.reminecraft.perms.listener.PlayerListener;
import org.reminecraft.perms.model.Group;

import java.util.*;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final ReminecraftPerms plugin;
    private final PlayerListener listener;

    public RankCommand(ReminecraftPerms plugin, PlayerListener listener) {
        this.plugin   = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setrank")) {
            return handleSet(sender, args);
        }

        if (args.length == 0) {
            if (!(sender instanceof Player p)) { err(sender, "Gunakan dari game."); return true; }
            showRank(sender, p);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "set"    -> handleSet(sender, Arrays.copyOfRange(args, 1, args.length));
            case "info"   -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list"   -> handleList(sender);
            default       -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) { showRank(sender, target); yield true; }
                err(sender, "Subcommand tidak dikenal. Gunakan: set, info, create, delete, list");
                yield true;
            }
        };
    }

    private void showRank(CommandSender sender, Player target) {
        Group g = PermsAPI.getPlayerGroup(target);
        List<String> extra = PermsAPI.getPlayerExtraPerms(target);
        send(sender, "§6▶ §e" + target.getName());
        send(sender, "  §7Group  : §f" + g.getName()
            + " §8(weight " + g.getWeight() + ")");
        send(sender, "  §7Prefix : " + g.getPrefix() + "example");
        send(sender, "  §7Perms  : §f" + g.getPermissions().size()
            + " group" + (extra.isEmpty() ? "" : " + " + extra.size() + " extra"));
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reminecraftperms.setrank")) {
            err(sender, "Tidak ada permission."); return true;
        }
        if (args.length < 2) { err(sender, "Usage: set <player> <group>"); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { err(sender, "Player " + args[0] + " tidak online."); return true; }

        String groupName = args[1].toLowerCase();
        if (PermsAPI.getGroup(groupName) == null) {
            err(sender, "Group §e" + groupName + " §ctidak ada. Lihat /rank list");
            return true;
        }

        PermsAPI.setPlayerGroup(target, groupName);
        listener.applyPermissions(target);

        send(sender, "§aRank §e" + target.getName() + " §adiset ke §6" + groupName);
        send(target, "§aRank kamu diubah ke §6" + groupName
            + " §7(prefix: " + PermsAPI.getGroup(groupName).getPrefix() + "example§7)");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { err(sender, "Usage: info <group>"); return true; }
        Group g = PermsAPI.getGroup(args[1]);
        if (g == null) { err(sender, "Group tidak ditemukan."); return true; }

        send(sender, "§6▶ Group: §e" + g.getName());
        send(sender, "  §7Weight : §f" + g.getWeight());
        send(sender, "  §7Prefix : " + g.getPrefix() + "example");
        send(sender, "  §7Perms  :");
        for (String p : g.getPermissions()) send(sender, "    §8- §f" + p);
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reminecraftperms.admin")) {
            err(sender, "Tidak ada permission."); return true;
        }
        if (args.length < 2) { err(sender, "Usage: create <name> [weight] [prefix]"); return true; }

        String name = args[1].toLowerCase();
        if (PermsAPI.getGroup(name) != null) { err(sender, "Group sudah ada."); return true; }

        int weight     = args.length > 2 ? parseInt(args[2], 0) : 0;
        String prefix  = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                                              .replace("&", "§") : "§7";

        PermsAPI.createGroup(name, weight, prefix);
        send(sender, "§aGroup §e" + name + " §adibuat. Edit permissions di §fgroups.yml §adan §f/rank reload §a(segera).");
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reminecraftperms.admin")) {
            err(sender, "Tidak ada permission."); return true;
        }
        if (args.length < 2) { err(sender, "Usage: delete <name>"); return true; }

        if (args[1].equalsIgnoreCase("default")) { err(sender, "Group default tidak bisa dihapus."); return true; }

        boolean ok = PermsAPI.deleteGroup(args[1]);
        if (ok) send(sender, "§aGroup §e" + args[1] + " §adihapus.");
        else    err(sender, "Group tidak ditemukan.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<Group> sorted = PermsAPI.getAllGroups().stream()
            .sorted(Comparator.comparingInt(Group::getWeight).reversed())
            .collect(Collectors.toList());

        send(sender, "§6▶ Daftar Group (" + sorted.size() + "):");
        for (Group g : sorted) {
            send(sender, "  §8" + g.getWeight() + " §f" + g.getName()
                + " §8| " + g.getPermissions().size() + " perms | prefix: "
                + g.getPrefix() + "example");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("set", "info", "create", "delete", "list"));
            Bukkit.getOnlinePlayers().forEach(p -> subs.add(p.getName()));
            return filter(subs, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(PermsAPI.getAllGroups().stream()
                .map(Group::getName).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete"))) {
            return filter(PermsAPI.getAllGroups().stream()
                .map(Group::getName).collect(Collectors.toList()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private void send(CommandSender s, String msg) { s.sendMessage(msg); }
    private void err(CommandSender s, String msg)  { s.sendMessage("§c" + msg); }
    private int  parseInt(String s, int def)       {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
