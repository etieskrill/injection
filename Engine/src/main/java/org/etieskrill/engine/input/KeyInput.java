package org.etieskrill.engine.input;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyInput {
    
    private final Type type;
    private final int value;
    private final int modifiers; //TODO modifiers matter not for equality or hashes - reconsider whether this is smart
    
    public static final int[] modifierKeys = {
            GLFW_KEY_LEFT_SHIFT,
            GLFW_KEY_RIGHT_SHIFT,
            GLFW_KEY_LEFT_ALT,
            GLFW_KEY_RIGHT_ALT,
            GLFW_KEY_CAPS_LOCK,
            GLFW_KEY_LEFT_CONTROL,
            GLFW_KEY_RIGHT_CONTROL,
            GLFW_KEY_NUM_LOCK
    };
    
    public enum Type {
        KEY,
        MOUSE
    }
    
    public KeyInput(Type type, int value) {
        this.type = type;
        this.value = value;
        this.modifiers = 0;
    }
    
    public KeyInput(Type type, int value, int modifiers) {
        this.type = type;
        this.value = value;
        this.modifiers = isModifier() ? 0 : modifiers;
    }
    
    public boolean isModifier() {
        return Arrays.stream(modifierKeys).anyMatch(key -> key == this.value);
    }
    
    public Type getType() {
        return type;
    }
    
    public int getValue() {
        return value;
    }
    
    public int getModifiers() {
        return modifiers;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        KeyInput that = (KeyInput) o;
        
        if (value != that.value) return false;
        if (modifiers != that.modifiers && !isModifier()) return false;
        return type == that.type;
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + value;
        if (!isModifier()) result = 31 * result + modifiers;
        return result;
    }
    
}
