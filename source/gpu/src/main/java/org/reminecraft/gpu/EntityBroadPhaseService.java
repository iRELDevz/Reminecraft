package org.reminecraft.gpu;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.List;

public final class EntityBroadPhaseService {

    public record Stats(boolean accelerated, String backend, int entities,
                        int pathfinding, long pairs, double lastMillis, int intervalTicks) {
    }

    private final Plugin plugin;
    private final ComputeEngine engine;
    private final NmsBridge nms;
    private final int intervalTicks;
    private final int maxEntities;

    private volatile boolean busy;
    private volatile int lastEntities;
    private volatile int lastPathfinding;
    private volatile long lastPairs;
    private volatile double lastMillis;
    private BukkitTask task;

    public EntityBroadPhaseService(Plugin plugin, ComputeEngine engine, NmsBridge nms,
                                   int intervalTicks, int maxEntities) {
        this.plugin        = plugin;
        this.engine        = engine;
        this.nms           = nms;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.maxEntities   = Math.max(64, maxEntities);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 100L, intervalTicks);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        if (busy) return;

        int cap = maxEntities;
        float[] boxes = new float[cap * 6];
        int count = 0;
        int pathfinding = 0;

        for (World world : plugin.getServer().getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (count >= cap) break;
                BoundingBox bb = entity.getBoundingBox();
                int b = count * 6;
                boxes[b]     = (float) bb.getMinX();
                boxes[b + 1] = (float) bb.getMinY();
                boxes[b + 2] = (float) bb.getMinZ();
                boxes[b + 3] = (float) bb.getMaxX();
                boxes[b + 4] = (float) bb.getMaxY();
                boxes[b + 5] = (float) bb.getMaxZ();
                if (nms.isPathfinding(entity)) pathfinding++;
                count++;
            }
            if (count >= cap) break;
        }

        if (count < 2) {
            lastEntities = count;
            lastPathfinding = pathfinding;
            lastPairs = 0;
            lastMillis = 0;
            return;
        }

        final int finalCount = count;
        final int finalPathfinding = pathfinding;
        busy = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long t0 = System.nanoTime();
                long pairs = engine.broadPhasePairs(boxes, finalCount);
                double ms = (System.nanoTime() - t0) / 1_000_000.0;
                lastEntities = finalCount;
                lastPathfinding = finalPathfinding;
                lastPairs = pairs;
                lastMillis = ms;
            } catch (Throwable t) {
                plugin.getLogger().warning("Broad-phase GPU pass gagal: " + t.getMessage());
            } finally {
                busy = false;
            }
        });
    }

    public Stats stats() {
        return new Stats(engine.accelerated(), engine.backend(), lastEntities,
                lastPathfinding, lastPairs, lastMillis, intervalTicks);
    }
}
