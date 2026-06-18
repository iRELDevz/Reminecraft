package org.reminecraft.auth;

import org.bukkit.plugin.java.JavaPlugin;

public class ReminecraftAuth extends JavaPlugin {

    private AuthStorage storage;
    private AuthManager mgr;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        storage = new AuthStorage(getDataFolder());
        mgr     = new AuthManager(storage, getConfig());

        AuthListener listener = new AuthListener(this, mgr);
        getServer().getPluginManager().registerEvents(listener, this);

        AuthCommand cmd = new AuthCommand(mgr, storage);
        for (String name : new String[]{"login", "register", "logout", "changepass", "auth"}) {
            var c = getCommand(name);
            if (c != null) c.setExecutor(cmd);
        }

        getLogger().info("ReminecraftAuth activo.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReminecraftAuth disabled.");
    }
}
