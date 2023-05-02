package org.etieskrill.game.settings;

import org.etieskrill.game.Action;
import org.etieskrill.game.view.Scene;

public interface KeyBindAPI {
    
    boolean fire(int key);
    
    boolean set(int key, Action action);
    
}
