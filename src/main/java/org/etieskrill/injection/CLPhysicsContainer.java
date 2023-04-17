package org.etieskrill.injection;

import org.etieskrill.injection.math.Vector2;
import org.etieskrill.injection.particle.Particle;
import org.jocl.Sizeof;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.etieskrill.lwjgl.opencl.demo.InfoUtil.getProgramBuildInfoInt;
import static org.etieskrill.lwjgl.opencl.demo.InfoUtil.getProgramBuildInfoStringASCII;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memUTF8;

public class CLPhysicsContainer {
    
    private static final int vectorDimension = 8;
    
    private final IntBuffer errorno = BufferUtils.createIntBuffer(1);
    
    private long platform;
    private long device;
    private long context;
    
    private long queue;
    private long program;
    private long kernel;
    
    private FloatBuffer posBuffer;
    private long clPosBuffer;
    private PointerBuffer workSize;
    private PointerBuffer event;
    
    private final Queue<Particle> particles;
    
    public CLPhysicsContainer(Queue<Particle> particles, int maxNumParticles) {
        this.particles = particles;
        
        initPlatformDeviceContextQueue();
        buildProgram("Vectors.cl");
        
        posBuffer = BufferUtils.createFloatBuffer(vectorDimension * maxNumParticles);
        clPosBuffer = clCreateBuffer(context, /*CL_MEM_READ_WRITE*/CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR, posBuffer, errorno);
        checkCLError(errorno.get(0));
        
        //kernel = clCreateKernel(program, "integrate", errorno);
        kernel = clCreateKernel(program, "sort", errorno);
        checkCLError(errorno.get(0));
    
        workSize = BufferUtils.createPointerBuffer(1);
        event = BufferUtils.createPointerBuffer(1);

        long clXSortedPosBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, Sizeof.cl_long, errorno);
        checkCLError(errorno.get(0));
        
        clSetKernelArg1p(kernel, 0, clPosBuffer);
        clSetKernelArg1i(kernel, 1, particles.size());
        clSetKernelArg1p(kernel, 2, clXSortedPosBuffer);
        clSetKernelArg1i(kernel, 3, 0);

        checkCLError(clEnqueueNDRangeKernel(queue, kernel, 1, null, workSize, null, null, event));
        checkCLError(clWaitForEvents(event.get(0)));

        FloatBuffer xSortedPosBuffer = BufferUtils.createFloatBuffer(vectorDimension * maxNumParticles);
        checkCLError(clEnqueueReadBuffer(queue, clXSortedPosBuffer, true, 0, xSortedPosBuffer, null, event));

        int i = 0;
        for (Particle particle : particles) {
            if (i >= maxNumParticles) break;

            if (!xSortedPosBuffer.hasRemaining()) {
                System.err.println("Could not find position data for particle at index " + i + ".");
                break;
            }
            particle.setPos(new Vector2(xSortedPosBuffer.get(), xSortedPosBuffer.get()));
            particle.setPosPrev(new Vector2(xSortedPosBuffer.get(), xSortedPosBuffer.get()));
            //TODO find a better solution for this hot garbage
            for (int j = 0; j < 4; j++) xSortedPosBuffer.get();
            i++;
        }

        for (Particle particle : particles) {
            System.out.println(particle.getPos());
        }
    }
    
    public static void main(String[] args) {
        Queue<Particle> ps = new ConcurrentLinkedQueue<>();
        ps.add(new Particle(3f, new Vector2(100f, 50f)));
        ps.add(new Particle(3f, new Vector2(50f, 50f)));
        ps.add(new Particle(3f, new Vector2(150f, 50f)));

        new CLPhysicsContainer(ps, 3);
    }
    
    //TODO separate platform, device and context init
    private void initPlatformDeviceContextQueue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(clGetPlatformIDs(null, pi));
            if (pi.get(0) == 0)
                throw new IllegalStateException("No OpenCL platforms found.");
        
            PointerBuffer platforms = stack.mallocPointer(pi.get(0));
            checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));
        
            platform = platforms.get(0);
        
            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, pi));
            PointerBuffer devices = stack.mallocPointer(pi.get(0));
            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices, (IntBuffer) null));
            device = devices.get(0);
        }
    
        PointerBuffer properties = BufferUtils.createPointerBuffer(4);
        properties.put(CL_CONTEXT_PLATFORM).put(platform).put(NULL).flip();
    
        CLContextCallbackI callback = CLContextCallback.create(
                ((errinfo, private_info, cb, user_data) -> System.err.printf("cl_context_callback: %s%n", memUTF8(errinfo)))
        );
        
        context = clCreateContext(properties, device, callback, NULL, errorno);
        checkCLError(errorno.get(0));
    
        queue = clCreateCommandQueue(context, device, NULL, errorno);
        checkCLError(errorno.get(0));
    }
    
    private void buildProgram(String name) {
        String source;
        try {
            byte[] bytes = Objects.requireNonNull(CLPhysicsContainer.class.getClassLoader().getResourceAsStream(name)).readAllBytes();
            source = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        program = clCreateProgramWithSource(context, source, errorno);
        checkCLError(errorno.get(0));
    
        CLProgramCallback programCallback = CLProgramCallback.create((clProgram, user_data) -> {
            System.err.printf("The cl_program [0x%X] was built %s%n", clProgram,
                    getProgramBuildInfoInt(clProgram, device, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"
            );
            String log = getProgramBuildInfoStringASCII(clProgram, device, CL_PROGRAM_BUILD_LOG);
            if (log.length() > 5) {
                System.err.printf("BUILD LOG:\n----\n%s\n-----%n", log);
            }
        });
    
        checkCLError(clBuildProgram(program, device, "", programCallback, NULL));
    }
    
    private void cleanup() {
        checkCLError(clReleaseMemObject(platform));
        checkCLError(clReleaseContext(context));
        checkCLError(clReleaseCommandQueue(queue));
        checkCLError(clReleaseProgram(program));
        checkCLError(clReleaseKernel(kernel));
        checkCLError(clReleaseMemObject(clPosBuffer));
        posBuffer = null;
        workSize = null;
        particles.clear();
        
        CL.destroy();
    }
    
    public void updatePosBuffer(float delta) {
        clSetKernelArg1p(kernel, 0, clPosBuffer);
        clSetKernelArg1i(kernel, 1, particles.size());
        clSetKernelArg1f(kernel, 2, delta);
        clSetKernelArg2f(kernel, 3, App.windowSize.getX(), App.windowSize.getY());
        
        workSize.put(0, particles.size());
        checkCLError(clEnqueueNDRangeKernel(queue, kernel, 1, null, workSize,
                null, null, event));
        checkCLError(clWaitForEvents(event));
    }
    
    public void pollPosBuffer(Queue<Particle> particles, int size) {
        checkCLError(clEnqueueReadBuffer(queue, clPosBuffer, true, 0, posBuffer, null, null));
        
        int i = 0;
        for (Particle particle : particles) {
            if (i >= size) break;
            
            if (!posBuffer.hasRemaining()) {
                System.err.println("Could not find position data for particle at index " + i + ".");
                break;
            }
            particle.setPos(new Vector2(posBuffer.get(), posBuffer.get()));
            particle.setPosPrev(new Vector2(posBuffer.get(), posBuffer.get()));
            //TODO find a better solution for this hot garbage
            for (int j = 0; j < 4; j++) posBuffer.get();
            i++;
        }
        
        posBuffer.rewind();
    }
    
    public void pollPosBuffer(Queue<Particle> particles) {
        pollPosBuffer(particles, particles.size());
    }
    
    public void setPosBuffer() {
        int x = 0;
        for (Particle particle : particles) {
            posBuffer.put(x, particle.getPos().getX()).put(x + 1, particle.getPos().getY())
                    .put(x + 2, particle.getPosPrev().getX()).put(x + 3, particle.getPosPrev().getY())
                    .put(x + 6, particle.getRadius());
            x += vectorDimension;
        }
        
        checkCLError(clEnqueueWriteBuffer(queue, clPosBuffer, true, 0, posBuffer, null, null));
    }
    
    public static void checkCLError(int errorCode) {
        if (errorCode != CL_SUCCESS)
            throw new RuntimeException(String.format("OpenCL error [%d]", errorCode));
    }
    
}
