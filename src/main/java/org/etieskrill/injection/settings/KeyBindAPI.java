package org.etieskrill.injection.settings;

import org.etieskrill.injection.Action;
import org.etieskrill.injection.view.Scene;

public interface KeyBindAPI {
    
    boolean fire(int key);
    
    boolean set(int key, Action action);
    
}
