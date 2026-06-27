__kernel void flow_relax(
        __global const uchar* passable,
        __global const float* in,
        __global float* out,
        const int width,
        const int height,
        __global volatile int* changed) {
    int gid = get_global_id(0);
    int total = width * height;
    if (gid >= total) return;

    if (!passable[gid]) {
        out[gid] = in[gid];
        return;
    }

    int x = gid % width;
    int y = gid / width;
    float best = in[gid];

    if (x > 0)          best = fmin(best, in[gid - 1]     + 1.0f);
    if (x < width - 1)  best = fmin(best, in[gid + 1]     + 1.0f);
    if (y > 0)          best = fmin(best, in[gid - width] + 1.0f);
    if (y < height - 1) best = fmin(best, in[gid + width] + 1.0f);

    out[gid] = best;
    if (best < in[gid]) atomic_inc(changed);
}
