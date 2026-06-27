__kernel void broadphase(
        __global const float* boxes,
        const int count,
        __global volatile int* outCount) {
    int i = get_global_id(0);
    if (i >= count) return;

    float aMinX = boxes[i * 6 + 0];
    float aMinY = boxes[i * 6 + 1];
    float aMinZ = boxes[i * 6 + 2];
    float aMaxX = boxes[i * 6 + 3];
    float aMaxY = boxes[i * 6 + 4];
    float aMaxZ = boxes[i * 6 + 5];

    int overlaps = 0;
    for (int j = i + 1; j < count; j++) {
        float bMinX = boxes[j * 6 + 0];
        float bMinY = boxes[j * 6 + 1];
        float bMinZ = boxes[j * 6 + 2];
        float bMaxX = boxes[j * 6 + 3];
        float bMaxY = boxes[j * 6 + 4];
        float bMaxZ = boxes[j * 6 + 5];

        bool hit = aMinX <= bMaxX && aMaxX >= bMinX
                && aMinY <= bMaxY && aMaxY >= bMinY
                && aMinZ <= bMaxZ && aMaxZ >= bMinZ;
        if (hit) overlaps++;
    }
    if (overlaps > 0) atomic_add(outCount, overlaps);
}
