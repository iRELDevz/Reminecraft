package org.reminecraft.gpu;

public record NoiseSettings(
        int seed,
        int octaves,
        float frequency,
        float lacunarity,
        float persistence) {

    public static NoiseSettings of(int seed, int octaves, double frequency,
                                   double lacunarity, double persistence) {
        return new NoiseSettings(
                seed,
                Math.max(1, octaves),
                (float) frequency,
                (float) lacunarity,
                (float) persistence);
    }
}
