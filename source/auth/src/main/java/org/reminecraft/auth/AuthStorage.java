package org.reminecraft.auth;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class AuthStorage {

    record PlayerData(String name, String hash, String salt, long registered, long lastLogin, String lastIp) {}

    private final File dir;
    private final ConcurrentHashMap<UUID, PlayerData> mem    = new ConcurrentHashMap<>();
    private final Set<UUID>                           absent = ConcurrentHashMap.newKeySet();

    AuthStorage(File pluginDir) {
        dir = new File(pluginDir, "data");
        dir.mkdirs();
    }

    PlayerData get(UUID uuid) {
        PlayerData cached = mem.get(uuid);
        if (cached != null) return cached;
        if (absent.contains(uuid)) return null;

        PlayerData loaded = read(uuid);
        if (loaded == null) {
            absent.add(uuid);
            return null;
        }
        PlayerData existing = mem.putIfAbsent(uuid, loaded);
        return existing != null ? existing : loaded;
    }

    boolean exists(UUID uuid) {
        return get(uuid) != null;
    }

    void put(UUID uuid, PlayerData data) {
        absent.remove(uuid);
        mem.put(uuid, data);
        CompletableFuture.runAsync(() -> write(uuid, data));
    }

    void touch(UUID uuid, long loginTime, String ip) {
        PlayerData d = get(uuid);
        if (d == null) return;
        put(uuid, new PlayerData(d.name(), d.hash(), d.salt(), d.registered(), loginTime, ip));
    }

    void evict(UUID uuid) {
        mem.remove(uuid);
    }

    void remove(UUID uuid) {
        mem.remove(uuid);
        absent.add(uuid);
        CompletableFuture.runAsync(() -> file(uuid).delete());
    }

    private PlayerData read(UUID uuid) {
        File f = file(uuid);
        if (!f.exists()) return null;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        return new PlayerData(
            y.getString("name", ""),
            y.getString("hash", ""),
            y.getString("salt", ""),
            y.getLong("registered"),
            y.getLong("lastLogin"),
            y.getString("ip", "")
        );
    }

    private void write(UUID uuid, PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name",       d.name());
        y.set("hash",       d.hash());
        y.set("salt",       d.salt());
        y.set("registered", d.registered());
        y.set("lastLogin",  d.lastLogin());
        y.set("ip",         d.lastIp());
        try { y.save(file(uuid)); }
        catch (IOException e) {
            System.err.println("[ReminecraftAuth] Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    private File file(UUID uuid) { return new File(dir, uuid + ".yml"); }
}
