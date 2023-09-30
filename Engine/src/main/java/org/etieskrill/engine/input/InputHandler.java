package org.etieskrill.engine.input;

@FunctionalInterface
public interface InputHandler {
    
    boolean invoke(Input.Type type, int key, int action, int modifiers);
    
}
