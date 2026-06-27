package org.reminecraft.gpu;

public record GpuDevice(
        int platformIndex,
        int deviceIndex,
        String name,
        String vendor,
        boolean gpu,
        long globalMemBytes,
        int computeUnits) {

    public long globalMemMB() {
        return globalMemBytes / (1024L * 1024L);
    }

    public String describe() {
        return name + " (" + vendor + ", " + (gpu ? "GPU" : "CPU/other")
                + ", " + globalMemMB() + " MB, " + computeUnits + " CU)";
    }
}
