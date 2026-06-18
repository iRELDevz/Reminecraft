package org.reminecraft.devmode;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ReminecraftDevmode extends JavaPlugin {

    private AntiBotManager antiBot;
    private ChatFilter chatFilter;
    private RateLimiter rateLimiter;
    private GroupMessages groupMessages;
    private MotdListener motdListener;
    private SuperlistManager superlist;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        antiBot       = new AntiBotManager(this);
        chatFilter    = new ChatFilter(this);
        rateLimiter   = new RateLimiter(this);
        groupMessages = new GroupMessages(this);
        motdListener  = new MotdListener(this);
        superlist     = new SuperlistManager(this);

        PluginManager pm = getServer().getPluginManager();
        if (getConfig().getBoolean("anti-bot.enabled"))       pm.registerEvents(antiBot, this);
        if (getConfig().getBoolean("chat-filter.enabled"))    pm.registerEvents(chatFilter, this);
        if (getConfig().getBoolean("rate-limiter.enabled"))   pm.registerEvents(rateLimiter, this);
        if (getConfig().getBoolean("group-messages.enabled")) pm.registerEvents(groupMessages, this);
        pm.registerEvents(motdListener, this);

        var devCmd = getCommand("devmode");
        if (devCmd != null) devCmd.setExecutor(new DevmodeCommand(this, antiBot, chatFilter, rateLimiter, groupMessages));

        var slCmd = getCommand("superlist");
        if (slCmd != null) slCmd.setExecutor(new SuperlistCommand(superlist));

        getLogger().info("ReminecraftDevmode enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReminecraftDevmode disabled.");
    }

    void reload() {
        reloadConfig();
        antiBot.reload();
        chatFilter.reload();
        rateLimiter.reload();
        groupMessages.reload();
        motdListener.reload();
        superlist.load();
    }
}
