package org.etieskrill.injection;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.Platform;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;

public class GLFWWindow {
    
    public static void main(String[] args) {
        glfwSetErrorCallback(GLFWErrorCallback.createPrint());
        if (!glfwInit()) {
            System.out.println("Unable to initialize glfw");
            System.exit(-1);
        }
    
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        }
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        //glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
    }
    
}
