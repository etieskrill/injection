package org.etieskrill.lwjgl.opencl;

import org.etieskrill.injection.math.Vector2;
import org.etieskrill.injection.particle.Particle;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.etieskrill.lwjgl.opencl.demo.InfoUtil.getProgramBuildInfoInt;
import static org.etieskrill.lwjgl.opencl.demo.InfoUtil.getProgramBuildInfoStringASCII;
import static org.jocl.Sizeof.cl_float2;
import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryUtil.*;

public class OpenCLApp {
    
    private static final Queue<Particle> particles = new ConcurrentLinkedQueue<>(List.of(
            new Particle(10f, new Vector2(100f, 100f)))
    );
    
    public static void main(String[] args) {
        long platform;
        CLCapabilities platformCapabilities;
        long device;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(clGetPlatformIDs(null, pi));
            if (pi.get(0) == 0)
                throw new IllegalStateException("No OpenCL platforms found.");
            
            PointerBuffer platforms = stack.mallocPointer(pi.get(0));
            checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));
            
            platform = platforms.get(0);
            platformCapabilities = CL.createPlatformCapabilities(platform);

            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, null, pi));
            PointerBuffer devices = stack.mallocPointer(pi.get(0));
            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices, (IntBuffer) null));
            device = devices.get(0);
        }
    
        List<float[]> list = new ArrayList<>();
        for (Particle particle : particles) {
            float[] floats = new float[]{particle.getPos().getX(), particle.getPos().getY()};
            list.add(floats);
        }
        float[][] posArr = list.toArray(new float[0][0]);
        //Pointer pos = Pointer.to(posArr);
        FloatBuffer posBuffer = BufferUtils.createFloatBuffer(2);
        posBuffer.put(0, posArr[0][0]).put(1, posArr[0][1]);
    
        PointerBuffer properties = BufferUtils.createPointerBuffer(4);
        properties.put(CL_CONTEXT_PLATFORM).put(platform).put(NULL).flip();
        
        CLContextCallbackI callback = CLContextCallback.create(
                ((errinfo, private_info, cb, user_data) -> System.err.printf("cl_context_callback: %s%n", memUTF8(errinfo)))
        );
        
        IntBuffer errorno = BufferUtils.createIntBuffer(1);
        long context = clCreateContext(properties, device, callback, NULL, errorno);
        checkCLError(errorno.get(0));

        long clPosBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, posBuffer, errorno);
        checkCLError(errorno.get(0));
        
        //long pPosCLMem = CL10.clCreateBuffer(context, (long) CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, posArr, errorno);
        //checkCLError(errorno.get(0));
        
        String source;
        try {
            byte[] bytes = Objects.requireNonNull(OpenCLApp.class.getClassLoader().getResourceAsStream("Vectors.cl")).readAllBytes();
            source = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long program = clCreateProgramWithSource(context, source, errorno);
        checkCLError(errorno.get(0));
        
        CLProgramCallback programCallback = CLProgramCallback.create((clProgram, user_data) -> {
            System.err.printf("The cl_program [0x%X] was built %s%n", clProgram,
                    getProgramBuildInfoInt(clProgram, device, CL_PROGRAM_BUILD_STATUS) == CL_SUCCESS ? "successfully" : "unsuccessfully"
            );
            String log = getProgramBuildInfoStringASCII(clProgram, device, CL_PROGRAM_BUILD_LOG);
            if (!log.isEmpty()) {
                System.err.printf("BUILD LOG:\n----\n%s\n-----%n", log);
            }
        });
        
        checkCLError(clBuildProgram(program, device, "", programCallback, NULL));
        
        long kernel = clCreateKernel(program, "print", errorno);
        checkCLError(errorno.get(0));
    
        long queue = clCreateCommandQueue(context, device, NULL, errorno);
        checkCLError(errorno.get(0));
        
        //clSetKernelArg2f(kernel, 0, posArr[0][0], posArr[0][1]);
        clSetKernelArg1p(kernel, 0, clPosBuffer);
        clSetKernelArg1i(kernel, 1, 69);
        clSetKernelArg1f(kernel, 2, 4.20f);


        
        //clEnqueueWriteBuffer(queue, pPosCLMem, CL_FALSE, 0, DATA_SIZE, pos, 0, NULL, NULL);
        //clEnqueueWriteBuffer(queue, pPosCLMem, false, 0L, pos.getNativePointer(), NULL, NULL);
        
        PointerBuffer workSize = BufferUtils.createPointerBuffer(1);
        PointerBuffer event = BufferUtils.createPointerBuffer(1);
        workSize.put(0, 1);
        checkCLError(clEnqueueNDRangeKernel(queue, kernel, 1, null, workSize,
                null, null, null));
        checkCLError(clEnqueueReadBuffer(queue, clPosBuffer, false, 0, posBuffer, null, event));
        checkCLError(clWaitForEvents(event));

        System.out.println(posBuffer.get(0) + " " + posBuffer.get(1));
    }
    
    private static void checkCLError(int errorCode) {
        if (errorCode != CL_SUCCESS)
            throw new RuntimeException(String.format("OpenCL error [%d]", errorCode));
    }
    
}
