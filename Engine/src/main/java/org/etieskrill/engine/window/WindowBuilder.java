package org.etieskrill.engine.window;

import glm_.vec2.Vec2;

import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;

public class WindowBuilder {
    
    private Window.WindowMode mode;
    private Window.WindowSize size;
    private final Vec2 position = new Vec2();
    private float refreshRate = GLFW_DONT_CARE;
    private String title;
    
    private WindowBuilder() {}
    
    public static WindowBuilder create() {
        return new WindowBuilder();
    }
    
    public WindowBuilder setMode(Window.WindowMode mode) {
        this.mode = mode;
        return this;
    }
    
    public WindowBuilder setSize(Window.WindowSize size) {
        this.size = size;
        return this;
    }
    
    public WindowBuilder setPosition(Vec2 position) {
        this.position.put(position);
        return this;
    }
    
    public WindowBuilder setRefreshRate(float refreshRate) {
        this.refreshRate = refreshRate;
        return this;
    }
    
    public WindowBuilder setTitle(String title) {
        this.title = title;
        return this;
    }
    
    public Window build() {
        return new Window(mode != null ? mode : Window.WindowMode.WINDOWED,
                size != null ? size : Window.WindowSize.LARGEST_FIT,
                position,
                refreshRate,
                title != null ? title : "Window");
    }
    
}
