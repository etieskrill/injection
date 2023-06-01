package org.etieskrill.engine.input;

import org.etieskrill.engine.scene._2d.Scene;

/**
 * Must expose a method of determining whether a certain input method is currently triggered, and register keybindings
 * to actions, where actions are scene-specific.
 */
public interface KeyBindAPI {
    
    boolean setCurrentContext(Scene scene);
    
    boolean registerAction(String name, Action action);
    boolean registerInputMethod(InputMethod inputMethod);
    
    boolean set(int key, Action action);
    boolean setByScancode(int scancode, Action action);
    
}
