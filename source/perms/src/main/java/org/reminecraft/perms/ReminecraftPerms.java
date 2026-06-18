package org.reminecraft.perms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.reminecraft.perms.api.PermsAPI;
import org.reminecraft.perms.command.RankCommand;
import org.reminecraft.perms.listener.PlayerListener;
import org.reminecraft.perms.storage.Storage;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class ReminecraftPerms extends JavaPlugin {

    private Storage storage;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();

        File defaultGroups = copyResource("groups.yml");

        storage = new Storage(getDataFolder());
        storage.load(defaultGroups);

        PermsAPI.init(storage);

        playerListener = new PlayerListener(this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);

        RankCommand rankCmd = new RankCommand(this, playerListener);
        getCommand("rank").setExecutor(rankCmd);
        getCommand("rank").setTabCompleter(rankCmd);
        getCommand("setrank").setExecutor(rankCmd);
        getCommand("setrank").setTabCompleter(rankCmd);

        for (Player p : Bukkit.getOnlinePlayers()) {
            PermsAPI.ensurePlayer(p);
            playerListener.applyPermissions(p);
        }

        getLogger().info("ReminecraftPerms enabled — "
            + storage.getAllGroups().size() + " groups loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReminecraftPerms disabled.");
    }

    private File copyResource(String name) {
        File out = new File(getDataFolder(), name);
        if (!out.exists()) {
            try (InputStream in = getResource(name)) {
                if (in != null) Files.copy(in, out.toPath());
            } catch (Exception e) {
                getLogger().warning("Could not copy " + name + ": " + e.getMessage());
            }
        }
        return out;
    }
}
