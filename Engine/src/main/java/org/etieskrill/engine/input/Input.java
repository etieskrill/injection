package org.etieskrill.engine.input;

public class Input {
    
    private final Type type;
    private final int value;
    private final int modifiers;
    
    public enum Type {
        KEY,
        MOUSE,
        CONTROLLER
    }
    
    public Input(Type type, int value) {
        this.type = type;
        this.value = value;
        this.modifiers = 0;
    }
    
    public Input(Type type, int value, int modifiers) {
        this.type = type;
        this.value = value;
        this.modifiers = modifiers;
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
        
        Input that = (Input) o;
        
        if (value != that.value) return false;
        return type == that.type;
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + value;
        return result;
    }
    
}
