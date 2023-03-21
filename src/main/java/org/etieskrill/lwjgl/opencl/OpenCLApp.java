package org.etieskrill.lwjgl.opencl;

import com.sun.javafx.application.ParametersImpl;
import org.etieskrill.injection.App;
import org.etieskrill.injection.math.Vector2;
import org.etieskrill.injection.particle.Particle;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;

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

public class OpenCLApp {
    
    private static final int vectorDimension = 8;
    
    private final IntBuffer errorno = BufferUtils.createIntBuffer(1);
    
    private long platform;
    private long device;
    private long context;
    
    private long queue;
    private long program;
    private long kernel;
    
    private final FloatBuffer posBuffer;
    private long clPosBuffer;
    
    private final Queue<Particle> particles;
    
    public OpenCLApp(Queue<Particle> particles, int maxNumParticles) {
        this.particles = particles;
        
        initPlatformDeviceContextQueue();
        buildProgram("Vectors.cl");
        
        posBuffer = BufferUtils.createFloatBuffer(vectorDimension * maxNumParticles);
        clPosBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, posBuffer, errorno);
        checkCLError(errorno.get(0));
        
        kernel = clCreateKernel(program, "integrate", errorno);
        checkCLError(errorno.get(0));
    
        /*for (int i = 0; i < 60; i++) {
            long start = System.nanoTime();
            updatePosBuffer(0.016f);
    
            checkCLError(clWaitForEvents(pollPosBuffer()));
            while (posBuffer.hasRemaining()) {
                System.out.print("sout: " + posBuffer.get() + " " + posBuffer.get());
            }
            posBuffer.rewind();
            System.out.println("cycle time: " + (System.nanoTime() - start) / 1000000d);
        }*/
    }
    
    /*public static void main(String[] args) {
        new OpenCLApp();
    }*/
    
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
            byte[] bytes = Objects.requireNonNull(OpenCLApp.class.getClassLoader().getResourceAsStream(name)).readAllBytes();
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
    
    public void updatePosBuffer(float delta) {
        clSetKernelArg1p(kernel, 0, clPosBuffer);
        clSetKernelArg1i(kernel, 1, particles.size());
        clSetKernelArg1f(kernel, 2, delta);
        clSetKernelArg2f(kernel, 3, App.windowSize.getX(), App.windowSize.getY());
        
        PointerBuffer workSize = BufferUtils.createPointerBuffer(1);
        workSize.put(0, particles.size());
        PointerBuffer event = BufferUtils.createPointerBuffer(1);
        checkCLError(clEnqueueNDRangeKernel(queue, kernel, 1, null, workSize,
                null, null, event));
        checkCLError(clWaitForEvents(event));
    }
    
    public PointerBuffer pollPosBuffer(Queue<Particle> particles) {
        PointerBuffer event = BufferUtils.createPointerBuffer(1);
        checkCLError(clEnqueueReadBuffer(queue, clPosBuffer, true, 0, posBuffer, null, event));
        
        int i = 0;
        int size = particles.size();
        
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
        return event;
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
