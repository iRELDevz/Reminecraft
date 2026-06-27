package org.reminecraft.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Benchmark {

    public record Row(String name, double cpuMs, double gpuMs, boolean verified, String detail) {
        public double speedup() {
            return gpuMs > 0 ? cpuMs / gpuMs : 0;
        }
    }

    private final int noiseSide;
    private final int collisionBoxes;
    private final int pathGrid;
    private final float tolerance;

    public Benchmark(int noiseSamples, int collisionBoxes, int pathGrid, double tolerance) {
        this.noiseSide      = Math.max(64, (int) Math.sqrt(Math.max(1, noiseSamples)));
        this.collisionBoxes = Math.max(64, collisionBoxes);
        this.pathGrid       = Math.max(32, pathGrid);
        this.tolerance      = (float) tolerance;
    }

    public List<Row> run(ComputeEngine cpu, ComputeEngine gpu) {
        List<Row> rows = new ArrayList<>();
        rows.add(noise(cpu, gpu));
        rows.add(collision(cpu, gpu));
        rows.add(pathfinding(cpu, gpu));
        return rows;
    }

    private Row noise(ComputeEngine cpu, ComputeEngine gpu) {
        NoiseSettings s = NoiseSettings.of(1337, 5, 0.0065, 2.0, 0.5);
        cpu.fractalNoise2D(s, 0, 0, 8, 8);

        long t0 = System.nanoTime();
        float[] cpuOut = cpu.fractalNoise2D(s, 0, 0, noiseSide, noiseSide);
        double cpuMs = ms(t0);

        if (gpu == null) {
            return new Row("Terrain noise", cpuMs, -1, false,
                    noiseSide + "x" + noiseSide + " titik, GPU n/a");
        }

        gpu.fractalNoise2D(s, 0, 0, 8, 8);
        long t1 = System.nanoTime();
        float[] gpuOut = gpu.fractalNoise2D(s, 0, 0, noiseSide, noiseSide);
        double gpuMs = ms(t1);

        float maxDiff = 0;
        for (int i = 0; i < cpuOut.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(cpuOut[i] - gpuOut[i]));
        }
        boolean ok = maxDiff <= tolerance;
        return new Row("Terrain noise", cpuMs, gpuMs, ok,
                noiseSide + "x" + noiseSide + " titik, max diff " + String.format("%.5f", maxDiff));
    }

    private Row collision(ComputeEngine cpu, ComputeEngine gpu) {
        float[] boxes = randomBoxes(collisionBoxes, 99L);
        cpu.broadPhasePairs(boxes, 64);

        long t0 = System.nanoTime();
        long cpuPairs = cpu.broadPhasePairs(boxes, collisionBoxes);
        double cpuMs = ms(t0);

        if (gpu == null) {
            return new Row("Entity collision", cpuMs, -1, false,
                    collisionBoxes + " AABB, " + cpuPairs + " overlap, GPU n/a");
        }

        gpu.broadPhasePairs(boxes, 64);
        long t1 = System.nanoTime();
        long gpuPairs = gpu.broadPhasePairs(boxes, collisionBoxes);
        double gpuMs = ms(t1);

        boolean ok = cpuPairs == gpuPairs;
        return new Row("Entity collision", cpuMs, gpuMs, ok,
                collisionBoxes + " AABB, " + cpuPairs + " overlap"
                        + (ok ? "" : " vs GPU " + gpuPairs));
    }

    private Row pathfinding(ComputeEngine cpu, ComputeEngine gpu) {
        int side = pathGrid;
        byte[] passable = randomGrid(side, 0.82, 7L);
        int goal = (side / 2) * side + (side / 2);
        passable[goal] = 1;
        int iterations = side * 4;

        cpu.flowFieldReached(passable, side, side, goal, iterations);

        long t0 = System.nanoTime();
        int cpuReached = cpu.flowFieldReached(passable, side, side, goal, iterations);
        double cpuMs = ms(t0);

        if (gpu == null) {
            return new Row("Mob pathfinding", cpuMs, -1, false,
                    side + "x" + side + " grid, " + cpuReached + " cell, GPU n/a");
        }

        gpu.flowFieldReached(passable, side, side, goal, iterations);
        long t1 = System.nanoTime();
        int gpuReached = gpu.flowFieldReached(passable, side, side, goal, iterations);
        double gpuMs = ms(t1);

        boolean ok = cpuReached == gpuReached;
        return new Row("Mob pathfinding", cpuMs, gpuMs, ok,
                side + "x" + side + " grid, " + cpuReached + " cell"
                        + (ok ? "" : " vs GPU " + gpuReached));
    }

    private static float[] randomBoxes(int count, long seed) {
        Random r = new Random(seed);
        float[] boxes = new float[count * 6];
        float span = (float) Math.cbrt(count) * 4.0f;
        for (int i = 0; i < count; i++) {
            float x = r.nextFloat() * span;
            float y = r.nextFloat() * 32.0f;
            float z = r.nextFloat() * span;
            float sx = 0.6f + r.nextFloat();
            float sy = 0.6f + r.nextFloat();
            float sz = 0.6f + r.nextFloat();
            int b = i * 6;
            boxes[b]     = x;
            boxes[b + 1] = y;
            boxes[b + 2] = z;
            boxes[b + 3] = x + sx;
            boxes[b + 4] = y + sy;
            boxes[b + 5] = z + sz;
        }
        return boxes;
    }

    private static byte[] randomGrid(int side, double passableChance, long seed) {
        Random r = new Random(seed);
        byte[] grid = new byte[side * side];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = (byte) (r.nextDouble() < passableChance ? 1 : 0);
        }
        return grid;
    }

    private static double ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
