package org.reminecraft.gpu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class GpuCommand implements CommandExecutor, TabCompleter {

    private final ReminecraftGPU plugin;

    public GpuCommand(ReminecraftGPU plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status"  -> status(sender);
            case "devices" -> devices(sender);
            case "bench"   -> bench(sender);
            case "mode"    -> mode(sender, args);
            default        -> sender.sendMessage(Component.text(
                    "Subcommand: status, devices, bench, mode <cpu|gpu>", NamedTextColor.RED));
        }
        return true;
    }

    private void status(CommandSender sender) {
        ComputeEngine active = plugin.activeEngine();
        header(sender, "GPU Engine Status");
        field(sender, "Mode config", plugin.configuredMode().name());
        field(sender, "Backend aktif", active.backend());
        field(sender, "Akselerasi", active.accelerated() ? "GPU aktif" : "CPU fallback",
                active.accelerated() ? NamedTextColor.GREEN : NamedTextColor.YELLOW);

        GpuComputeEngine gpu = plugin.gpuEngine();
        if (gpu != null) {
            field(sender, "GPU device", gpu.device().describe());
        } else {
            field(sender, "GPU device", "tidak terdeteksi / OpenCL tidak tersedia", NamedTextColor.GRAY);
        }

        field(sender, "Terrain GPU", plugin.terrainEnabled()
                ? "aktif -> world '" + plugin.terrainWorld() + "'" : "nonaktif");
        field(sender, "Offload collision", plugin.offloadCollision() ? "on" : "off");
        field(sender, "Offload pathfinding", plugin.offloadPathfinding() ? "on" : "off");
    }

    private void devices(CommandSender sender) {
        header(sender, "OpenCL Devices");
        List<GpuDevice> devices;
        try {
            devices = GpuComputeEngine.enumerate();
        } catch (Throwable t) {
            sender.sendMessage(Component.text("OpenCL tidak tersedia di sistem ini: "
                    + t.getMessage(), NamedTextColor.RED));
            return;
        }
        if (devices.isEmpty()) {
            sender.sendMessage(Component.text("Tidak ada device OpenCL ditemukan.", NamedTextColor.GRAY));
            return;
        }
        for (GpuDevice d : devices) {
            sender.sendMessage(Component.text("  [" + d.platformIndex() + ":" + d.deviceIndex() + "] ",
                            NamedTextColor.AQUA)
                    .append(Component.text(d.describe(),
                            d.gpu() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text("Pilih lewat config: device.platform-index / device.device-index",
                NamedTextColor.DARK_GRAY));
    }

    private void bench(CommandSender sender) {
        ComputeEngine cpu = plugin.cpuEngine();
        GpuComputeEngine gpu = plugin.gpuEngine();
        sender.sendMessage(Component.text("Menjalankan benchmark (async), tunggu sebentar...",
                NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Benchmark.Row> rows;
            try {
                rows = plugin.benchmark().run(cpu, gpu);
            } catch (Throwable t) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        Component.text("Benchmark gagal: " + t.getMessage(), NamedTextColor.RED)));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> renderBench(sender, rows));
        });
    }

    private void renderBench(CommandSender sender, List<Benchmark.Row> rows) {
        header(sender, "Benchmark CPU vs GPU");
        for (Benchmark.Row r : rows) {
            Component head = Component.text("  " + r.name() + "  ", NamedTextColor.WHITE)
                    .append(verifyTag(r));
            sender.sendMessage(head);

            String cpu = String.format(Locale.ROOT, "%.2f ms", r.cpuMs());
            String gpu = r.gpuMs() > 0 ? String.format(Locale.ROOT, "%.2f ms", r.gpuMs()) : "n/a";
            Component detail = Component.text("    CPU " + cpu + "   GPU " + gpu, NamedTextColor.GRAY);
            if (r.gpuMs() > 0) {
                NamedTextColor c = r.speedup() >= 1 ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
                detail = detail.append(Component.text(
                        String.format(Locale.ROOT, "   %.2fx", r.speedup()), c));
            }
            sender.sendMessage(detail);
            sender.sendMessage(Component.text("    " + r.detail(), NamedTextColor.DARK_GRAY));
        }
    }

    private Component verifyTag(Benchmark.Row r) {
        if (r.gpuMs() <= 0) return Component.text("[CPU only]", NamedTextColor.GRAY);
        return r.verified()
                ? Component.text("[hasil cocok]", NamedTextColor.GREEN)
                : Component.text("[MISMATCH]", NamedTextColor.RED);
    }

    private void mode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /gpu mode <cpu|gpu|auto>", NamedTextColor.RED));
            return;
        }
        ComputeMode mode = ComputeMode.parse(args[1]);
        plugin.getConfig().set("compute-mode", mode.name().toLowerCase(Locale.ROOT));
        plugin.saveConfig();
        sender.sendMessage(Component.text("Mode compute diset ke " + mode.name()
                + ". Restart server untuk menerapkan ke terrain generation.", NamedTextColor.GREEN));
    }

    private void header(CommandSender sender, String title) {
        sender.sendMessage(Component.text("▶ " + title, NamedTextColor.GOLD));
    }

    private void field(CommandSender sender, String key, String value) {
        field(sender, key, value, NamedTextColor.WHITE);
    }

    private void field(CommandSender sender, String key, String value, NamedTextColor valueColor) {
        sender.sendMessage(Component.text("  " + key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, valueColor)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("status", "devices", "bench", "mode")
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return Stream.of("cpu", "gpu", "auto")
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
