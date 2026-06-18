package org.reminecraft.devmode;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChatFilter implements Listener {

    private final ReminecraftDevmode plugin;
    private volatile List<Pattern> patterns = List.of();
    private volatile String replacement;
    private volatile String action;

    ChatFilter(ReminecraftDevmode plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        var cfg = plugin.getConfig();
        replacement = cfg.getString("chat-filter.replacement", "***");
        action      = cfg.getString("chat-filter.action", "replace");

        List<Pattern> built = new ArrayList<>();
        for (String p : cfg.getStringList("chat-filter.patterns")) {
            try { built.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)); }
            catch (Exception ignored) {}
        }
        patterns = List.copyOf(built);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onChat(AsyncChatEvent e) {
        List<Pattern> snap = patterns;
        if (snap.isEmpty()) return;

        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        boolean matched = false;
        boolean cancel = "cancel".equals(action);

        for (Pattern p : snap) {
            Matcher m = p.matcher(text);
            if (cancel) {
                if (m.find()) { e.setCancelled(true); return; }
            } else {
                String replaced = m.replaceAll(replacement);
                if (!replaced.equals(text)) { text = replaced; matched = true; }
            }
        }

        if (matched) e.message(Component.text(text));
    }

    void addPattern(String regex) {
        try {
            List<Pattern> next = new ArrayList<>(patterns);
            next.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            patterns = List.copyOf(next);
        } catch (Exception ignored) {}
    }

    void removePattern(String regex) {
        List<Pattern> next = new ArrayList<>(patterns);
        next.removeIf(p -> p.pattern().equals(regex));
        patterns = List.copyOf(next);
    }

    List<Pattern> getPatterns() { return patterns; }
}
