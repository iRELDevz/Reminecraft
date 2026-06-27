package org.reminecraft.gpu.daemon;

import org.reminecraft.gpu.ComputeEngine;
import org.reminecraft.gpu.CpuComputeEngine;
import org.reminecraft.gpu.GpuComputeEngine;
import org.reminecraft.gpu.NoiseSettings;
import org.reminecraft.gpu.RemoteProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GpuDaemon {

    public static void main(String[] args) throws IOException {
        int port = arg(args, 0, 25599);
        String bind = args.length > 1 ? args[1] : "0.0.0.0";
        int platformIndex = arg(args, 2, -1);
        int deviceIndex = arg(args, 3, -1);

        ComputeEngine engine;
        try {
            engine = GpuComputeEngine.create(platformIndex, deviceIndex, true);
            System.out.println("[daemon] backend: " + engine.backend());
        } catch (Throwable t) {
            engine = new CpuComputeEngine();
            System.out.println("[daemon] GPU tidak tersedia, pakai CPU: " + t.getMessage());
        }
        final ComputeEngine eng = engine;

        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(bind, port));
        System.out.println("[daemon] ReminecraftGPU daemon listening on " + bind + ":" + port);
        System.out.println("[daemon] PENTING: jalankan lewat tunnel privat (Tailscale/WireGuard), jangan expose ke publik.");

        ExecutorService pool = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.close(); } catch (IOException ignored) {}
            pool.shutdownNow();
            eng.close();
        }));

        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                pool.submit(() -> handle(client, eng));
            } catch (IOException e) {
                if (!server.isClosed()) System.err.println("[daemon] accept error: " + e.getMessage());
            }
        }
    }

    private static void handle(Socket socket, ComputeEngine engine) {
        try (Socket s = socket) {
            s.setSoTimeout(15000);
            s.setTcpNoDelay(true);
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            if (in.readInt() != RemoteProtocol.MAGIC) {
                writeError(out, "magic salah");
                return;
            }
            byte op = in.readByte();
            if (op != RemoteProtocol.OP_NOISE) {
                writeError(out, "opcode tidak dikenal: " + op);
                return;
            }

            int originX = in.readInt();
            int originZ = in.readInt();
            int width = in.readInt();
            int height = in.readInt();
            int seed = in.readInt();
            int octaves = in.readInt();
            float frequency = in.readFloat();
            float lacunarity = in.readFloat();
            float persistence = in.readFloat();

            if (width <= 0 || height <= 0 || (long) width * height > RemoteProtocol.MAX_POINTS) {
                writeError(out, "dimensi tidak valid: " + width + "x" + height);
                return;
            }

            NoiseSettings settings = NoiseSettings.of(seed, octaves, frequency, lacunarity, persistence);
            float[] result = engine.fractalNoise2D(settings, originX, originZ, width, height);

            out.writeByte(RemoteProtocol.STATUS_OK);
            out.writeInt(result.length);
            for (float v : result) out.writeFloat(v);
            out.flush();
        } catch (Throwable t) {
            System.err.println("[daemon] request error: " + t.getMessage());
        }
    }

    private static void writeError(DataOutputStream out, String message) {
        try {
            out.writeByte(RemoteProtocol.STATUS_ERR);
            out.writeUTF(message);
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private static int arg(String[] args, int index, int def) {
        if (index >= args.length) return def;
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
