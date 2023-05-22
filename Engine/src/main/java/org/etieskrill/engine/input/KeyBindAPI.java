package org.etieskrill.engine.input;

public interface KeyBindAPI {
    
    boolean invoke(int key);
    
    boolean set(int key, Action action);
    
}
