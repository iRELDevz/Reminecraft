package org.reminecraft.gpu;

public enum ComputeMode {
    CPU,
    GPU,
    REMOTE;

    public static ComputeMode parse(String raw) {
        if (raw == null) return GPU;
        return switch (raw.trim().toLowerCase()) {
            case "cpu"    -> CPU;
            case "remote" -> REMOTE;
            default       -> GPU;
        };
    }
}
