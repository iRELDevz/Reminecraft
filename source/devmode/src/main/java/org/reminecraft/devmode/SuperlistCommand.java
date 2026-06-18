package org.reminecraft.devmode;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class SuperlistCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 8;

    private final SuperlistManager mgr;

    SuperlistCommand(SuperlistManager mgr) {
        this.mgr = mgr;
    }

    record KnownPlayer(String name, UUID uuid) {}

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!isAllowed(s)) {
            s.sendMessage(Component.text("Kamu tidak ada di superlist.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) { showList(s, 0); return true; }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /superlist add <uuid|name>", NamedTextColor.RED)); return true; }
                UUID uuid = resolve(args[1]);
                if (uuid == null) { s.sendMessage(Component.text("Player tidak ditemukan: " + args[1], NamedTextColor.RED)); return true; }
                mgr.add(uuid);
                s.sendMessage(Component.text("Ditambahkan ke superlist: " + args[1], NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 2) { s.sendMessage(Component.text("Usage: /superlist remove <uuid|name>", NamedTextColor.RED)); return true; }
                UUID uuid = resolve(args[1]);
                if (uuid == null) { s.sendMessage(Component.text("Player tidak ditemukan: " + args[1], NamedTextColor.RED)); return true; }
                mgr.remove(uuid);
                s.sendMessage(Component.text("Dihapus dari superlist: " + args[1], NamedTextColor.RED));
            }
            case "page" -> {
                int page = 0;
                if (args.length > 1) { try { page = Integer.parseInt(args[1]) - 1; } catch (Exception ignored) {} }
                showList(s, page);
            }
            default -> {
                try { showList(s, Integer.parseInt(args[0]) - 1); }
                catch (Exception ignored) { showList(s, 0); }
            }
        }
        return true;
    }

    private void showList(CommandSender s, int page) {
        List<KnownPlayer> all = loadKnown();
        if (all.isEmpty()) {
            s.sendMessage(Component.text("Belum ada player yang pernah join.", NamedTextColor.GRAY));
            return;
        }

        int total = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, total - 1));

        s.sendMessage(Component.text("━━━ SuperList Manager ━━━", NamedTextColor.AQUA, TextDecoration.BOLD));

        int from = page * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, all.size());

        for (int i = from; i < to; i++) {
            KnownPlayer kp = all.get(i);
            boolean member = mgr.isMember(kp.uuid());

            Component status = member
                ? Component.text("✓ ", NamedTextColor.GREEN)
                : Component.text("  ", NamedTextColor.DARK_GRAY);

            Component action = member
                ? Component.text(" [Remove]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/superlist remove " + kp.uuid()))
                    .hoverEvent(HoverEvent.showText(Component.text("Hapus " + kp.name() + " dari superlist")))
                : Component.text(" [Add]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/superlist add " + kp.uuid()))
                    .hoverEvent(HoverEvent.showText(Component.text("Tambah " + kp.name() + " ke superlist")));

            s.sendMessage(status
                .append(Component.text(kp.name(), member ? NamedTextColor.WHITE : NamedTextColor.GRAY))
                .append(action));
        }

        Component nav = Component.empty();
        if (page > 0)
            nav = nav.append(Component.text("[◀]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/superlist page " + page))
                .hoverEvent(HoverEvent.showText(Component.text("Halaman " + page))));
        nav = nav.append(Component.text("  Hal " + (page + 1) + "/" + total + "  ", NamedTextColor.GRAY));
        if (page < total - 1)
            nav = nav.append(Component.text("[▶]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/superlist page " + (page + 2)))
                .hoverEvent(HoverEvent.showText(Component.text("Halaman " + (page + 2)))));

        s.sendMessage(nav);
        s.sendMessage(Component.text("Superlist aktif: " + mgr.members().size() + " player", NamedTextColor.GRAY));
    }

    private List<KnownPlayer> loadKnown() {
        File f = new File("usercache.json");
        if (!f.exists()) return List.of();
        try {
            String json = Files.readString(f.toPath());
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            List<KnownPlayer> list = new ArrayList<>(arr.size());
            for (var el : arr) {
                var obj = el.getAsJsonObject();
                String name = obj.get("name").getAsString();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                list.add(new KnownPlayer(name, uuid));
            }
            list.sort((a, b) -> {
                boolean am = mgr.isMember(a.uuid()), bm = mgr.isMember(b.uuid());
                if (am != bm) return am ? -1 : 1;
                return a.name().compareToIgnoreCase(b.name());
            });
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isAllowed(CommandSender s) {
        if (!(s instanceof Player p)) return true;
        return mgr.isMember(p.getUniqueId());
    }

    private UUID resolve(String input) {
        try { return UUID.fromString(input); } catch (Exception ignored) {}
        List<KnownPlayer> all = loadKnown();
        for (KnownPlayer kp : all)
            if (kp.name().equalsIgnoreCase(input)) return kp.uuid();
        return null;
    }
}
