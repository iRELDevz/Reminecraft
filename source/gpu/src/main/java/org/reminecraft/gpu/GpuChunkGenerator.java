package org.reminecraft.gpu;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.LongFunction;

public final class GpuChunkGenerator extends ChunkGenerator {

    private final ComputeEngine engine;
    private final NoiseSettings settings;
    private final int seaLevel;
    private final int baseHeight;
    private final int heightAmplitude;
    private final int regionChunks;
    private final int regionCols;
    private final Map<Long, int[]> cache;

    public GpuChunkGenerator(ComputeEngine engine, NoiseSettings settings, int seaLevel,
                             int baseHeight, int heightAmplitude, int regionChunks, int cacheRegions) {
        this.engine          = engine;
        this.settings        = settings;
        this.seaLevel        = seaLevel;
        this.baseHeight      = baseHeight;
        this.heightAmplitude = heightAmplitude;
        this.regionChunks    = Math.max(1, regionChunks);
        this.regionCols      = this.regionChunks * 16;
        int cap = Math.max(4, cacheRegions);
        this.cache = new LinkedHashMap<>(cap * 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
                return size() > cap;
            }
        };
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        int minY = data.getMinHeight();
        int maxY = data.getMaxHeight();

        int regionX = Math.floorDiv(chunkX, regionChunks);
        int regionZ = Math.floorDiv(chunkZ, regionChunks);
        int[] heights = region(regionX, regionZ);

        int baseCol = (chunkX - regionX * regionChunks) * 16;
        int baseRow = (chunkZ - regionZ * regionChunks) * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int idx = (baseRow + z) * regionCols + (baseCol + x);
                int height = clamp(heights[idx], minY + 2, maxY - 2);

                data.setBlock(x, minY, z, Material.BEDROCK);
                data.setRegion(x, minY + 1, z, x + 1, height - 2, z + 1, Material.STONE);
                data.setRegion(x, height - 2, z, x + 1, height, z + 1, Material.DIRT);

                if (height < seaLevel) {
                    data.setBlock(x, height, z, Material.SAND);
                    data.setRegion(x, height + 1, z, x + 1, seaLevel + 1, z + 1, Material.WATER);
                } else {
                    data.setBlock(x, height, z, Material.GRASS_BLOCK);
                }
            }
        }
    }

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
        return heightAt(x, z);
    }

    private int heightAt(int x, int z) {
        float n = CpuComputeEngine.fractal(x, z, settings);
        return baseHeight + Math.round((n - 0.5f) * heightAmplitude);
    }

    private int[] region(int regionX, int regionZ) {
        long key = (((long) regionX) << 32) ^ (regionZ & 0xFFFFFFFFL);
        return computeIfAbsent(key, k -> build(regionX, regionZ));
    }

    private int[] build(int regionX, int regionZ) {
        int originX = regionX * regionCols;
        int originZ = regionZ * regionCols;
        float[] noise = engine.fractalNoise2D(settings, originX, originZ, regionCols, regionCols);
        int[] heights = new int[noise.length];
        for (int i = 0; i < noise.length; i++) {
            heights[i] = baseHeight + Math.round((noise[i] - 0.5f) * heightAmplitude);
        }
        return heights;
    }

    private int[] computeIfAbsent(long key, LongFunction<int[]> supplier) {
        synchronized (cache) {
            int[] cached = cache.get(key);
            if (cached != null) return cached;
        }
        int[] computed = supplier.apply(key);
        synchronized (cache) {
            int[] existing = cache.get(key);
            if (existing != null) return existing;
            cache.put(key, computed);
            return computed;
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
