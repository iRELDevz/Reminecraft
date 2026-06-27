package org.reminecraft.gpu;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public final class CpuComputeEngine implements ComputeEngine {

    @Override
    public String backend() {
        return "CPU (Java, " + Runtime.getRuntime().availableProcessors() + " threads)";
    }

    @Override
    public boolean accelerated() {
        return false;
    }

    @Override
    public float[] fractalNoise2D(NoiseSettings s, int originX, int originZ, int width, int height) {
        float[] out = new float[width * height];
        IntStream.range(0, height).parallel().forEach(lz -> {
            int row = lz * width;
            for (int lx = 0; lx < width; lx++) {
                out[row + lx] = fractal(originX + lx, originZ + lz, s);
            }
        });
        return out;
    }

    @Override
    public long broadPhasePairs(float[] boxes, int count) {
        AtomicLong total = new AtomicLong();
        IntStream.range(0, count).parallel().forEach(i -> {
            int bi = i * 6;
            float aMinX = boxes[bi],     aMinY = boxes[bi + 1], aMinZ = boxes[bi + 2];
            float aMaxX = boxes[bi + 3], aMaxY = boxes[bi + 4], aMaxZ = boxes[bi + 5];
            long local = 0;
            for (int j = i + 1; j < count; j++) {
                int bj = j * 6;
                if (aMinX <= boxes[bj + 3] && aMaxX >= boxes[bj]
                 && aMinY <= boxes[bj + 4] && aMaxY >= boxes[bj + 1]
                 && aMinZ <= boxes[bj + 5] && aMaxZ >= boxes[bj + 2]) {
                    local++;
                }
            }
            if (local > 0) total.addAndGet(local);
        });
        return total.get();
    }

    @Override
    public int flowFieldReached(byte[] passable, int width, int height, int goalIndex, int maxIterations) {
        int total = width * height;
        boolean[] seen = new boolean[total];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if (goalIndex < 0 || goalIndex >= total || passable[goalIndex] == 0) return 0;
        seen[goalIndex] = true;
        queue.add(goalIndex);
        int reached = 0;
        while (!queue.isEmpty()) {
            int idx = queue.poll();
            reached++;
            int x = idx % width;
            int y = idx / width;
            if (x > 0)          step(idx - 1,     passable, seen, queue);
            if (x < width - 1)  step(idx + 1,     passable, seen, queue);
            if (y > 0)          step(idx - width, passable, seen, queue);
            if (y < height - 1) step(idx + width, passable, seen, queue);
        }
        return reached;
    }

    private static void step(int idx, byte[] passable, boolean[] seen, ArrayDeque<Integer> queue) {
        if (!seen[idx] && passable[idx] != 0) {
            seen[idx] = true;
            queue.add(idx);
        }
    }

    @Override
    public void close() {
    }

    static float fractal(float x, float z, NoiseSettings s) {
        float freq = s.frequency();
        float amp  = 1.0f;
        float sum  = 0.0f;
        float norm = 0.0f;
        for (int o = 0; o < s.octaves(); o++) {
            int os = s.seed() + o * 0x9E3779B9;
            sum  += value(x * freq, z * freq, os) * amp;
            norm += amp;
            freq *= s.lacunarity();
            amp  *= s.persistence();
        }
        return sum / norm;
    }

    private static float value(float x, float z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        float u = fx * fx * (3.0f - 2.0f * fx);
        float v = fz * fz * (3.0f - 2.0f * fz);
        float n00 = unit(hash(x0,     z0,     seed));
        float n10 = unit(hash(x0 + 1, z0,     seed));
        float n01 = unit(hash(x0,     z0 + 1, seed));
        float n11 = unit(hash(x0 + 1, z0 + 1, seed));
        float nx0 = n00 + u * (n10 - n00);
        float nx1 = n01 + u * (n11 - n01);
        return nx0 + v * (nx1 - nx0);
    }

    private static int hash(int xi, int zi, int seed) {
        int h = seed + 0x9E3779B9;
        h ^= xi * 0x85EBCA6B;
        h  = h ^ (h >>> 13);
        h ^= zi * 0xC2B2AE35;
        h  = (h ^ (h >>> 16)) * 0x27D4EB2F;
        h ^= h >>> 15;
        return h;
    }

    private static float unit(int h) {
        return (h & 0x00FFFFFF) * (1.0f / 16777216.0f);
    }
}
