package org.etieskrill.engine.input;

public interface KeyBindAPI {
    
    boolean invoke(int key);
    //boolean setCurrentContext(Scene scene);
    
    //boolean registerAction(String name, Action action);
    //boolean registerInputMethod(InputMethod inputMethod);
    
    boolean set(int key, Action action);
    //boolean setByScancode(int scancode, Action action);
    
}
