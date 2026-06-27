package org.reminecraft.gpu;

public interface ComputeEngine extends AutoCloseable {

    String backend();

    boolean accelerated();

    float[] fractalNoise2D(NoiseSettings settings, int originX, int originZ, int width, int height);

    long broadPhasePairs(float[] boxes, int count);

    int flowFieldReached(byte[] passable, int width, int height, int goalIndex, int maxIterations);

    @Override
    void close();
}
