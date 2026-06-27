package org.reminecraft.gpu;

import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReminecraftGPU extends JavaPlugin {

    private ComputeMode configuredMode;
    private CpuComputeEngine cpu;
    private GpuComputeEngine gpu;
    private RemoteComputeEngine remote;
    private ComputeEngine active;
    private Benchmark benchmark;
    private GpuChunkGenerator terrainGenerator;
    private NmsBridge nms;
    private EntityBroadPhaseService broadPhase;

    private boolean terrainEnabled;
    private String terrainWorld;
    private boolean offloadCollision;
    private boolean offloadPathfinding;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuredMode     = ComputeMode.parse(getConfig().getString("compute-mode", "auto"));
        offloadCollision   = getConfig().getBoolean("offload.collision", true);
        offloadPathfinding = getConfig().getBoolean("offload.pathfinding", true);

        cpu = new CpuComputeEngine();
        initEngine();
        active = remote != null ? remote : (gpu != null ? gpu : cpu);

        benchmark = new Benchmark(
                getConfig().getInt("benchmark.noise-samples", 1048576),
                getConfig().getInt("benchmark.collision-boxes", 8192),
                getConfig().getInt("benchmark.pathfinding-grid", 256),
                getConfig().getDouble("benchmark.verify-tolerance", 0.002));

        terrainGenerator = buildGenerator();

        var cmd = getCommand("gpu");
        if (cmd != null) {
            GpuCommand executor = new GpuCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("ReminecraftGPU enabled. Backend: " + active.backend());

        nms = NmsBridge.probe(getLogger());
        if (offloadCollision && gpu != null) {
            broadPhase = new EntityBroadPhaseService(this, active, nms,
                    getConfig().getInt("offload.broadphase-interval-ticks", 40),
                    getConfig().getInt("offload.max-entities", 4096));
            broadPhase.start();
            getLogger().info("Entity broad-phase GPU offload aktif (interval "
                    + getConfig().getInt("offload.broadphase-interval-ticks", 40) + " tick).");
        }

        terrainEnabled = getConfig().getBoolean("terrain.enabled", false);
        terrainWorld   = getConfig().getString("terrain.world", "gpu_world");
        if (terrainEnabled) createTerrainWorld();
    }

    @Override
    public void onDisable() {
        if (broadPhase != null) broadPhase.stop();
        if (gpu != null) {
            try { gpu.close(); } catch (Throwable ignored) {}
        }
        if (cpu != null) cpu.close();
        getLogger().info("ReminecraftGPU disabled.");
    }

    public RemoteComputeEngine remoteEngine() {
        return remote;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return terrainGenerator;
    }

    private void initEngine() {
        if (configuredMode == ComputeMode.CPU) {
            getLogger().info("compute-mode=cpu, GPU dilewati.");
            return;
        }
        if (configuredMode == ComputeMode.REMOTE) {
            remote = new RemoteComputeEngine(
                    getConfig().getString("remote.host", "127.0.0.1"),
                    getConfig().getInt("remote.port", 25599),
                    getConfig().getInt("remote.timeout-ms", 4000),
                    getConfig().getLong("remote.reconnect-cooldown-ms", 30000),
                    cpu,
                    getLogger());
            remote.probe();
            return;
        }
        boolean preferGpu = !"cpu".equalsIgnoreCase(getConfig().getString("device.prefer", "gpu"));
        int platformIndex = getConfig().getInt("device.platform-index", -1);
        int deviceIndex   = getConfig().getInt("device.device-index", -1);
        try {
            gpu = GpuComputeEngine.create(platformIndex, deviceIndex, preferGpu);
            getLogger().info("GPU engine siap: " + gpu.device().describe());
        } catch (Throwable t) {
            gpu = null;
            String reason = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            if (configuredMode == ComputeMode.GPU) {
                getLogger().warning("compute-mode=gpu tapi GPU gagal init, fallback ke CPU: " + reason);
            } else {
                getLogger().info("GPU tidak tersedia, pakai CPU: " + reason);
            }
        }
    }

    private GpuChunkGenerator buildGenerator() {
        NoiseSettings settings = NoiseSettings.of(
                getConfig().getInt("terrain.seed", 0),
                getConfig().getInt("terrain.octaves", 5),
                getConfig().getDouble("terrain.frequency", 0.0065),
                getConfig().getDouble("terrain.lacunarity", 2.0),
                getConfig().getDouble("terrain.persistence", 0.5));
        return new GpuChunkGenerator(
                active,
                settings,
                getConfig().getInt("terrain.sea-level", 63),
                getConfig().getInt("terrain.base-height", 64),
                getConfig().getInt("terrain.height-amplitude", 48),
                getConfig().getInt("terrain.region-chunks", 8),
                getConfig().getInt("terrain.cache-regions", 64));
    }

    private void createTerrainWorld() {
        getLogger().info("Membuat GPU terrain world '" + terrainWorld + "'...");
        try {
            new WorldCreator(terrainWorld).generator(terrainGenerator).createWorld();
            getLogger().info("GPU terrain world '" + terrainWorld + "' siap.");
        } catch (Throwable t) {
            getLogger().warning("Gagal membuat GPU terrain world: " + t.getMessage());
        }
    }

    public ComputeMode configuredMode()   { return configuredMode; }
    public ComputeEngine activeEngine()   { return active; }
    public CpuComputeEngine cpuEngine()   { return cpu; }
    public GpuComputeEngine gpuEngine()   { return gpu; }
    public Benchmark benchmark()          { return benchmark; }
    public boolean terrainEnabled()       { return terrainEnabled; }
    public String terrainWorld()          { return terrainWorld; }
    public boolean offloadCollision()     { return offloadCollision; }
    public boolean offloadPathfinding()   { return offloadPathfinding; }
    public NmsBridge nmsBridge()          { return nms; }
    public EntityBroadPhaseService broadPhaseService() { return broadPhase; }
}
