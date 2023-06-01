package org.etieskrill.engine.input;

import org.etieskrill.engine.scene._2d.Scene;

import java.util.HashMap;
import java.util.Map;

public class KeyBind implements KeyBindAPI {
    
    private final Map<Integer, Action> keyBinds;
    
    public KeyBind() {
        this.keyBinds = new HashMap<>();
        //TODO initialise default binds (escape menu)
    }

    protected boolean invoke(int key) {
        Action action = keyBinds.get(key);
        if (action == null) return false;
        action.run();
        return true;
    }

    @Override
    public boolean setCurrentContext(Scene scene) {
        return false;
    }

    @Override
    public boolean registerAction(String name, Action action) {
        return false;
    }

    @Override
    public boolean registerInputMethod(InputMethod inputMethod) {
        return false;
    }

    @Override
    public boolean set(int key, Action action) {
        keyBinds.put(key, action);
        return true;
    }

    @Override
    public boolean setByScancode(int scancode, Action action) {
        return false;
    }

}
