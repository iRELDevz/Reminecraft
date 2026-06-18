package org.reminecraft.auth;

import org.bukkit.configuration.file.FileConfiguration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class AuthManager {

    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();
    private static final ThreadLocal<MessageDigest> DIGEST = ThreadLocal.withInitial(() -> {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    });

    private final AuthStorage storage;
    private final long sessionMs;
    private final int minLen;
    private final int maxLen;
    private final int maxAttempts;

    private final Set<UUID> authed   = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bedrock  = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();

    AuthManager(AuthStorage storage, FileConfiguration cfg) {
        this.storage     = storage;
        this.sessionMs   = cfg.getLong("session-hours", 12) * 3_600_000L;
        this.minLen      = cfg.getInt("min-password-length", 5);
        this.maxLen      = cfg.getInt("max-password-length", 32);
        this.maxAttempts = cfg.getInt("max-attempts", 5);
    }

    boolean trySession(UUID uuid, String ip) {
        AuthStorage.PlayerData d = storage.get(uuid);
        if (d == null) return false;
        if (System.currentTimeMillis() - d.lastLogin() < sessionMs && ip.equals(d.lastIp())) {
            authed.add(uuid);
            return true;
        }
        return false;
    }

    boolean login(UUID uuid, String name, String password, String ip) {
        AuthStorage.PlayerData d = storage.get(uuid);
        if (d == null) return false;
        if (!verify(password, d.salt(), d.hash())) {
            attempts.merge(uuid, 1, Integer::sum);
            return false;
        }
        attempts.remove(uuid);
        authed.add(uuid);
        storage.touch(uuid, System.currentTimeMillis(), ip);
        return true;
    }

    boolean register(UUID uuid, String name, String password, String ip) {
        if (storage.exists(uuid)) return false;
        String salt = salt();
        long now = System.currentTimeMillis();
        storage.put(uuid, new AuthStorage.PlayerData(name, hash(password, salt), salt, now, now, ip));
        authed.add(uuid);
        return true;
    }

    boolean changePassword(UUID uuid, String oldPass, String newPass) {
        AuthStorage.PlayerData d = storage.get(uuid);
        if (d == null || !verify(oldPass, d.salt(), d.hash())) return false;
        String salt = salt();
        storage.put(uuid, new AuthStorage.PlayerData(d.name(), hash(newPass, salt), salt, d.registered(), System.currentTimeMillis(), d.lastIp()));
        return true;
    }

    void markBedrock(UUID uuid) { bedrock.add(uuid); authed.add(uuid); }
    void logout(UUID uuid)      { authed.remove(uuid); }

    void cleanup(UUID uuid) {
        authed.remove(uuid);
        bedrock.remove(uuid);
        attempts.remove(uuid);
        storage.evict(uuid);
    }

    boolean isAuthed(UUID uuid)     { return authed.contains(uuid); }
    boolean isBedrock(UUID uuid)    { return bedrock.contains(uuid); }
    boolean isRegistered(UUID uuid) { return storage.exists(uuid); }
    int     attempts(UUID uuid)     { return attempts.getOrDefault(uuid, 0); }
    int     maxAttempts()           { return maxAttempts; }
    int     minLen()                { return minLen; }
    int     maxLen()                { return maxLen; }

    void unregister(UUID uuid) {
        authed.remove(uuid);
        storage.remove(uuid);
    }

    String validatePassword(String pass) {
        if (pass.length() < minLen) return "Password minimal " + minLen + " karakter.";
        if (pass.length() > maxLen) return "Password maksimal " + maxLen + " karakter.";
        return null;
    }

    private static String salt() {
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return HEX.formatHex(b);
    }

    private static String hash(String pass, String salt) {
        MessageDigest md = DIGEST.get();
        md.reset();
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        md.update(pass.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(md.digest());
    }

    private static boolean verify(String pass, String salt, String storedHex) {
        MessageDigest md = DIGEST.get();
        md.reset();
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        md.update(pass.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(md.digest(), HEX.parseHex(storedHex));
    }
}
