inline uint rm_hash(int xi, int zi, uint seed) {
    uint h = seed + 0x9E3779B9u;
    h ^= (uint)xi * 0x85EBCA6Bu;
    h  = h ^ (h >> 13);
    h ^= (uint)zi * 0xC2B2AE35u;
    h  = (h ^ (h >> 16)) * 0x27D4EB2Fu;
    h ^= h >> 15;
    return h;
}

inline float rm_unit(uint h) {
    return (float)(h & 0x00FFFFFFu) * (1.0f / 16777216.0f);
}

inline float rm_value(float x, float z, uint seed) {
    int x0 = (int)floor(x);
    int z0 = (int)floor(z);
    float fx = x - (float)x0;
    float fz = z - (float)z0;
    float u = fx * fx * (3.0f - 2.0f * fx);
    float v = fz * fz * (3.0f - 2.0f * fz);
    float n00 = rm_unit(rm_hash(x0,     z0,     seed));
    float n10 = rm_unit(rm_hash(x0 + 1, z0,     seed));
    float n01 = rm_unit(rm_hash(x0,     z0 + 1, seed));
    float n11 = rm_unit(rm_hash(x0 + 1, z0 + 1, seed));
    float nx0 = mix(n00, n10, u);
    float nx1 = mix(n01, n11, u);
    return mix(nx0, nx1, v);
}

inline float rm_fractal(float x, float z, uint seed, int octaves,
                        float frequency, float lacunarity, float persistence) {
    float freq = frequency;
    float amp  = 1.0f;
    float sum  = 0.0f;
    float norm = 0.0f;
    for (int o = 0; o < octaves; o++) {
        uint os = seed + (uint)o * 0x9E3779B9u;
        sum  += rm_value(x * freq, z * freq, os) * amp;
        norm += amp;
        freq *= lacunarity;
        amp  *= persistence;
    }
    return sum / norm;
}

__kernel void fractal_noise(
        __global float* out,
        const int originX,
        const int originZ,
        const int width,
        const uint seed,
        const int octaves,
        const float frequency,
        const float lacunarity,
        const float persistence) {
    int gid = get_global_id(0);
    int lx = gid % width;
    int lz = gid / width;
    float wx = (float)(originX + lx);
    float wz = (float)(originZ + lz);
    out[gid] = rm_fractal(wx, wz, seed, octaves, frequency, lacunarity, persistence);
}
