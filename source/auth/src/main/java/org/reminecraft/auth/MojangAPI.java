package org.reminecraft.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class MojangAPI {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long TTL = 30 * 60_000L;

    private record CachedResult(UUID uuid, long ts) {}

    static CompletableFuture<UUID> fetchUUID(String name) {
        String key = name.toLowerCase();
        CachedResult hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.ts() < TTL)
            return CompletableFuture.completedFuture(hit.uuid());

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
            .timeout(Duration.ofSeconds(5))
            .GET().build();

        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(r -> {
                if (r.statusCode() != 200) {
                    cache.put(key, new CachedResult(null, System.currentTimeMillis()));
                    return null;
                }
                try {
                    JsonObject obj = JsonParser.parseString(r.body()).getAsJsonObject();
                    UUID uuid = dashless(obj.get("id").getAsString());
                    cache.put(key, new CachedResult(uuid, System.currentTimeMillis()));
                    return uuid;
                } catch (Exception e) {
                    return null;
                }
            })
            .exceptionally(e -> null);
    }

    static boolean isPremiumName(String name) {
        CachedResult hit = cache.get(name.toLowerCase());
        return hit != null && hit.uuid() != null;
    }

    private static UUID dashless(String s) {
        return UUID.fromString(s.substring(0,8)+"-"+s.substring(8,12)+"-"+s.substring(12,16)+"-"+s.substring(16,20)+"-"+s.substring(20));
    }
}
