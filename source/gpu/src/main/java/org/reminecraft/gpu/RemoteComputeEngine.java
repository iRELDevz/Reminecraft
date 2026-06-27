package org.reminecraft.gpu;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

public final class RemoteComputeEngine implements ComputeEngine {

    private final String host;
    private final int port;
    private final int timeoutMs;
    private final long cooldownMs;
    private final ComputeEngine fallback;
    private final Logger logger;

    private volatile boolean remoteOk;
    private volatile long downUntil;

    public RemoteComputeEngine(String host, int port, int timeoutMs, long cooldownMs,
                               ComputeEngine fallback, Logger logger) {
        this.host       = host;
        this.port       = port;
        this.timeoutMs  = Math.max(250, timeoutMs);
        this.cooldownMs = Math.max(1000, cooldownMs);
        this.fallback   = fallback;
        this.logger     = logger;
    }

    public void probe() {
        try {
            remoteNoise(NoiseSettings.of(0, 1, 0.01, 2.0, 0.5), 0, 0, 1, 1);
            remoteOk = true;
            logger.info("Remote GPU daemon terhubung di " + host + ":" + port + ".");
        } catch (IOException e) {
            remoteOk = false;
            downUntil = System.currentTimeMillis() + cooldownMs;
            logger.warning("Remote GPU daemon belum terjangkau (" + host + ":" + port
                    + "): " + e.getMessage() + " - terrain pakai CPU sampai daemon hidup.");
        }
    }

    @Override
    public String backend() {
        return "Remote GPU " + host + ":" + port + (remoteOk ? " (terhubung)" : " (fallback CPU)");
    }

    @Override
    public boolean accelerated() {
        return remoteOk;
    }

    @Override
    public float[] fractalNoise2D(NoiseSettings s, int originX, int originZ, int width, int height) {
        if (System.currentTimeMillis() < downUntil) {
            return fallback.fractalNoise2D(s, originX, originZ, width, height);
        }
        try {
            float[] result = remoteNoise(s, originX, originZ, width, height);
            if (!remoteOk) {
                remoteOk = true;
                logger.info("Remote GPU daemon pulih, terrain kembali ke GPU.");
            }
            return result;
        } catch (IOException e) {
            if (remoteOk) {
                logger.warning("Remote GPU daemon putus: " + e.getMessage() + " - fallback CPU.");
            }
            remoteOk = false;
            downUntil = System.currentTimeMillis() + cooldownMs;
            return fallback.fractalNoise2D(s, originX, originZ, width, height);
        }
    }

    @Override
    public long broadPhasePairs(float[] boxes, int count) {
        return fallback.broadPhasePairs(boxes, count);
    }

    @Override
    public int flowFieldReached(byte[] passable, int width, int height, int goalIndex, int maxIterations) {
        return fallback.flowFieldReached(passable, width, height, goalIndex, maxIterations);
    }

    @Override
    public void close() {
        fallback.close();
    }

    private float[] remoteNoise(NoiseSettings s, int originX, int originZ, int width, int height)
            throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            socket.setTcpNoDelay(true);

            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeInt(RemoteProtocol.MAGIC);
            out.writeByte(RemoteProtocol.OP_NOISE);
            out.writeInt(originX);
            out.writeInt(originZ);
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(s.seed());
            out.writeInt(s.octaves());
            out.writeFloat(s.frequency());
            out.writeFloat(s.lacunarity());
            out.writeFloat(s.persistence());
            out.flush();

            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            byte status = in.readByte();
            if (status != RemoteProtocol.STATUS_OK) {
                throw new IOException("daemon: " + in.readUTF());
            }
            int n = in.readInt();
            if (n != width * height) {
                throw new IOException("ukuran balasan tidak sesuai: " + n);
            }
            float[] result = new float[n];
            for (int i = 0; i < n; i++) result[i] = in.readFloat();
            return result;
        }
    }
}
