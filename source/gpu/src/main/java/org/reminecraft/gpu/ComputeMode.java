package org.reminecraft.gpu;

public enum ComputeMode {
    CPU,
    GPU,
    REMOTE,
    AUTO;

    public static ComputeMode parse(String raw) {
        if (raw == null) return AUTO;
        return switch (raw.trim().toLowerCase()) {
            case "cpu"    -> CPU;
            case "gpu"    -> GPU;
            case "remote" -> REMOTE;
            default       -> AUTO;
        };
    }
}
