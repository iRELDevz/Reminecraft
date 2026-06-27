package org.reminecraft.gpu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class KernelSource {

    private static final String[] FILES = {
            "kernels/noise.cl",
            "kernels/collision.cl",
            "kernels/flowfield.cl"
    };

    private KernelSource() {
    }

    static String combined() {
        StringBuilder sb = new StringBuilder();
        for (String file : FILES) {
            sb.append(read(file)).append('\n');
        }
        return sb.toString();
    }

    private static String read(String resource) {
        try (InputStream in = KernelSource.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("kernel resource hilang: " + resource);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) buf.write(chunk, 0, read);
            return buf.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memuat kernel " + resource, e);
        }
    }
}
