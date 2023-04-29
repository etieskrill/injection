package org.etieskrill.injection.settings;

import org.etieskrill.injection.Action;

import java.util.HashMap;
import java.util.Map;

public class KeyBind implements KeyBindAPI {
    
    private final Map<Integer, Action> keyBinds;
    
    public KeyBind() {
        this.keyBinds = new HashMap<>();
        //TODO initialise default binds (escape menu)
    }
    
    @Override
    public boolean fire(int key) {
        Action action = keyBinds.get(key);
        if (action == null) return false;
        action.run();
        return true;
    }
    
    @Override
    public boolean set(int key, Action action) {
        keyBinds.put(key, action);
        return true;
    }
    
}
