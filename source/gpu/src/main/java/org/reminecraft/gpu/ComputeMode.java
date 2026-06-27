package org.reminecraft.gpu;

public enum ComputeMode {
    CPU,
    GPU,
    AUTO;

    public static ComputeMode parse(String raw) {
        if (raw == null) return AUTO;
        return switch (raw.trim().toLowerCase()) {
            case "cpu"  -> CPU;
            case "gpu"  -> GPU;
            default     -> AUTO;
        };
    }
}
