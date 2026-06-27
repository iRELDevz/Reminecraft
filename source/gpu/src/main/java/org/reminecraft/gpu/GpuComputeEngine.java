package org.reminecraft.gpu;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

import java.util.ArrayList;
import java.util.List;

import static org.jocl.CL.*;

public final class GpuComputeEngine implements ComputeEngine {

    private final GpuDevice device;
    private final cl_context context;
    private final cl_command_queue queue;
    private final cl_program program;
    private final cl_kernel noiseKernel;
    private final cl_kernel collisionKernel;
    private final cl_kernel flowKernel;
    private final Object lock = new Object();

    private GpuComputeEngine(GpuDevice device, cl_context context, cl_command_queue queue,
                             cl_program program, cl_kernel noise, cl_kernel collision, cl_kernel flow) {
        this.device          = device;
        this.context         = context;
        this.queue           = queue;
        this.program         = program;
        this.noiseKernel     = noise;
        this.collisionKernel = collision;
        this.flowKernel      = flow;
    }

    public GpuDevice device() {
        return device;
    }

    @Override
    public String backend() {
        return "GPU: " + device.describe();
    }

    @Override
    public boolean accelerated() {
        return true;
    }

    @Override
    public float[] fractalNoise2D(NoiseSettings s, int originX, int originZ, int width, int height) {
        int n = width * height;
        float[] out = new float[n];
        synchronized (lock) {
            cl_mem outMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                    (long) Sizeof.cl_float * n, null, null);
            try {
                clSetKernelArg(noiseKernel, 0, Sizeof.cl_mem,   Pointer.to(outMem));
                clSetKernelArg(noiseKernel, 1, Sizeof.cl_int,   Pointer.to(new int[]{originX}));
                clSetKernelArg(noiseKernel, 2, Sizeof.cl_int,   Pointer.to(new int[]{originZ}));
                clSetKernelArg(noiseKernel, 3, Sizeof.cl_int,   Pointer.to(new int[]{width}));
                clSetKernelArg(noiseKernel, 4, Sizeof.cl_uint,  Pointer.to(new int[]{s.seed()}));
                clSetKernelArg(noiseKernel, 5, Sizeof.cl_int,   Pointer.to(new int[]{s.octaves()}));
                clSetKernelArg(noiseKernel, 6, Sizeof.cl_float, Pointer.to(new float[]{s.frequency()}));
                clSetKernelArg(noiseKernel, 7, Sizeof.cl_float, Pointer.to(new float[]{s.lacunarity()}));
                clSetKernelArg(noiseKernel, 8, Sizeof.cl_float, Pointer.to(new float[]{s.persistence()}));
                clEnqueueNDRangeKernel(queue, noiseKernel, 1, null,
                        new long[]{n}, null, 0, null, null);
                clEnqueueReadBuffer(queue, outMem, CL_TRUE, 0,
                        (long) Sizeof.cl_float * n, Pointer.to(out), 0, null, null);
            } finally {
                clReleaseMemObject(outMem);
            }
        }
        return out;
    }

    @Override
    public long broadPhasePairs(float[] boxes, int count) {
        int[] counter = {0};
        synchronized (lock) {
            cl_mem boxMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) Sizeof.cl_float * 6 * count, Pointer.to(boxes), null);
            cl_mem cntMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                    Sizeof.cl_int, Pointer.to(counter), null);
            try {
                clSetKernelArg(collisionKernel, 0, Sizeof.cl_mem, Pointer.to(boxMem));
                clSetKernelArg(collisionKernel, 1, Sizeof.cl_int, Pointer.to(new int[]{count}));
                clSetKernelArg(collisionKernel, 2, Sizeof.cl_mem, Pointer.to(cntMem));
                clEnqueueNDRangeKernel(queue, collisionKernel, 1, null,
                        new long[]{count}, null, 0, null, null);
                clEnqueueReadBuffer(queue, cntMem, CL_TRUE, 0,
                        Sizeof.cl_int, Pointer.to(counter), 0, null, null);
            } finally {
                clReleaseMemObject(boxMem);
                clReleaseMemObject(cntMem);
            }
        }
        return counter[0] & 0xFFFFFFFFL;
    }

    @Override
    public int flowFieldReached(byte[] passable, int width, int height, int goalIndex, int maxIterations) {
        int total = width * height;
        if (goalIndex < 0 || goalIndex >= total || passable[goalIndex] == 0) return 0;

        final float inf = 1.0e9f;
        float[] dist = new float[total];
        for (int i = 0; i < total; i++) dist[i] = inf;
        dist[goalIndex] = 0.0f;

        float[] result = new float[total];
        int[] changed = {0};

        synchronized (lock) {
            cl_mem passMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) Sizeof.cl_char * total, Pointer.to(passable), null);
            cl_mem bufA = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                    (long) Sizeof.cl_float * total, Pointer.to(dist), null);
            cl_mem bufB = clCreateBuffer(context, CL_MEM_READ_WRITE,
                    (long) Sizeof.cl_float * total, null, null);
            cl_mem chgMem = clCreateBuffer(context, CL_MEM_READ_WRITE,
                    Sizeof.cl_int, null, null);
            try {
                cl_mem in = bufA;
                cl_mem out = bufB;
                int cap = Math.min(maxIterations, total);
                for (int it = 0; it < cap; it++) {
                    changed[0] = 0;
                    clEnqueueWriteBuffer(queue, chgMem, CL_TRUE, 0,
                            Sizeof.cl_int, Pointer.to(changed), 0, null, null);
                    clSetKernelArg(flowKernel, 0, Sizeof.cl_mem, Pointer.to(passMem));
                    clSetKernelArg(flowKernel, 1, Sizeof.cl_mem, Pointer.to(in));
                    clSetKernelArg(flowKernel, 2, Sizeof.cl_mem, Pointer.to(out));
                    clSetKernelArg(flowKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{width}));
                    clSetKernelArg(flowKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{height}));
                    clSetKernelArg(flowKernel, 5, Sizeof.cl_mem, Pointer.to(chgMem));
                    clEnqueueNDRangeKernel(queue, flowKernel, 1, null,
                            new long[]{total}, null, 0, null, null);
                    clEnqueueReadBuffer(queue, chgMem, CL_TRUE, 0,
                            Sizeof.cl_int, Pointer.to(changed), 0, null, null);
                    cl_mem swap = in;
                    in = out;
                    out = swap;
                    if (changed[0] == 0) break;
                }
                clEnqueueReadBuffer(queue, in, CL_TRUE, 0,
                        (long) Sizeof.cl_float * total, Pointer.to(result), 0, null, null);
            } finally {
                clReleaseMemObject(passMem);
                clReleaseMemObject(bufA);
                clReleaseMemObject(bufB);
                clReleaseMemObject(chgMem);
            }
        }

        int reached = 0;
        for (int i = 0; i < total; i++) {
            if (result[i] < inf * 0.5f) reached++;
        }
        return reached;
    }

    @Override
    public void close() {
        synchronized (lock) {
            clReleaseKernel(noiseKernel);
            clReleaseKernel(collisionKernel);
            clReleaseKernel(flowKernel);
            clReleaseProgram(program);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
        }
    }

    public static List<GpuDevice> enumerate() {
        List<GpuDevice> result = new ArrayList<>();
        int[] numPlatforms = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);
        if (numPlatforms[0] == 0) return result;

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        for (int p = 0; p < platforms.length; p++) {
            int[] numDevices = new int[1];
            try {
                clGetDeviceIDs(platforms[p], CL_DEVICE_TYPE_ALL, 0, null, numDevices);
            } catch (RuntimeException e) {
                continue;
            }
            if (numDevices[0] == 0) continue;

            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platforms[p], CL_DEVICE_TYPE_ALL, devices.length, devices, null);

            for (int d = 0; d < devices.length; d++) {
                result.add(describe(p, d, devices[d]));
            }
        }
        return result;
    }

    public static GpuComputeEngine create(int platformIndex, int deviceIndex, boolean preferGpu) {
        setExceptionsEnabled(true);

        int[] numPlatforms = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);
        if (numPlatforms[0] == 0) throw new IllegalStateException("Tidak ada OpenCL platform.");

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        cl_platform_id platform = null;
        cl_device_id device = null;
        GpuDevice meta = null;

        if (platformIndex >= 0 && deviceIndex >= 0 && platformIndex < platforms.length) {
            platform = platforms[platformIndex];
            cl_device_id[] devices = devicesOf(platform);
            if (deviceIndex < devices.length) {
                device = devices[deviceIndex];
                meta = describe(platformIndex, deviceIndex, device);
            }
        }

        if (device == null) {
            long wanted = preferGpu ? CL_DEVICE_TYPE_GPU : CL_DEVICE_TYPE_ALL;
            for (int p = 0; p < platforms.length && device == null; p++) {
                cl_device_id[] devices = devicesOf(platforms[p], wanted);
                if (devices.length > 0) {
                    platform = platforms[p];
                    device = devices[0];
                    meta = describe(p, 0, device);
                }
            }
        }

        if (device == null) {
            for (int p = 0; p < platforms.length && device == null; p++) {
                cl_device_id[] devices = devicesOf(platforms[p]);
                if (devices.length > 0) {
                    platform = platforms[p];
                    device = devices[0];
                    meta = describe(p, 0, device);
                }
            }
        }

        if (device == null) throw new IllegalStateException("Tidak ada OpenCL device yang bisa dipakai.");

        cl_context_properties props = new cl_context_properties();
        props.addProperty(CL_CONTEXT_PLATFORM, platform);
        cl_context context = clCreateContext(props, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueue(context, device, 0, null);

        String source = KernelSource.combined();
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{source}, null, null);
        try {
            clBuildProgram(program, 0, null, null, null, null);
        } catch (RuntimeException e) {
            String log = buildLog(program, device);
            clReleaseProgram(program);
            clReleaseCommandQueue(queue);
            clReleaseContext(context);
            throw new IllegalStateException("Build kernel OpenCL gagal:\n" + log, e);
        }

        cl_kernel noise = clCreateKernel(program, "fractal_noise", null);
        cl_kernel collision = clCreateKernel(program, "broadphase", null);
        cl_kernel flow = clCreateKernel(program, "flow_relax", null);

        return new GpuComputeEngine(meta, context, queue, program, noise, collision, flow);
    }

    private static cl_device_id[] devicesOf(cl_platform_id platform) {
        return devicesOf(platform, CL_DEVICE_TYPE_ALL);
    }

    private static cl_device_id[] devicesOf(cl_platform_id platform, long type) {
        int[] num = new int[1];
        try {
            clGetDeviceIDs(platform, type, 0, null, num);
        } catch (RuntimeException e) {
            return new cl_device_id[0];
        }
        if (num[0] == 0) return new cl_device_id[0];
        cl_device_id[] devices = new cl_device_id[num[0]];
        clGetDeviceIDs(platform, type, devices.length, devices, null);
        return devices;
    }

    private static GpuDevice describe(int platformIndex, int deviceIndex, cl_device_id device) {
        String name = infoString(device, CL_DEVICE_NAME);
        String vendor = infoString(device, CL_DEVICE_VENDOR);
        long type = infoLong(device, CL_DEVICE_TYPE);
        long mem = infoLong(device, CL_DEVICE_GLOBAL_MEM_SIZE);
        int units = (int) infoLong(device, CL_DEVICE_MAX_COMPUTE_UNITS);
        boolean gpu = (type & CL_DEVICE_TYPE_GPU) != 0;
        return new GpuDevice(platformIndex, deviceIndex, name, vendor, gpu, mem, units);
    }

    private static String infoString(cl_device_id device, int param) {
        long[] size = new long[1];
        clGetDeviceInfo(device, param, 0, null, size);
        byte[] buffer = new byte[(int) size[0]];
        clGetDeviceInfo(device, param, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, Math.max(0, buffer.length - 1)).trim();
    }

    private static long infoLong(cl_device_id device, int param) {
        long[] value = new long[1];
        clGetDeviceInfo(device, param, Sizeof.cl_ulong, Pointer.to(value), null);
        return value[0];
    }

    private static String buildLog(cl_program program, cl_device_id device) {
        long[] size = new long[1];
        clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, null, size);
        byte[] buffer = new byte[(int) size[0]];
        clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, Math.max(0, buffer.length - 1));
    }
}
