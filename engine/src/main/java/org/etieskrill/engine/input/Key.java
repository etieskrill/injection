package org.etieskrill.engine.input;

public class Key {
    
    private final Type type;
    private final int value;
    private final boolean modifier;
    private final int modifiers;
    
    public enum Type {
        KEYBOARD,
        MOUSE
    }

    public Key(Type type, int value) {
        this(type, value, 0);
    }

    public Key(Type type, int value, int modifiers) {
        this.type = type;
        this.value = value;
        this.modifier = (modifiers & 0x32) != 0;
        this.modifiers = isModifier() ? 0 : modifiers;
    }
    
    public Type getType() {
        return type;
    }
    
    public int getValue() {
        return value;
    }
    
    public boolean isModifier() {
        return modifier;
    }
    
    public int getModifiers() {
        return modifiers;
    }

    public Key withoutModifiers() {
        return new Key(type, value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || (getClass() != o.getClass() && o.getClass() != Keys.class)) return false;

        Key that = o.getClass() == Keys.class ? ((Keys) o).getInput() : (Key) o;
        
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
    
    @Override
    public String toString() {
        return "KeyInput{" +
                "type=" + type +
                ", value=" + value +
                ", modifiers=" + modifiers +
                ", modifier=" + modifier +
                '}';
    }
    
}
