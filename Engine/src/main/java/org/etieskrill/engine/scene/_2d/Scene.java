package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.util.UniqueIDManager;
import org.etieskrill.engine.input.KeyBind;
import org.etieskrill.engine.input.KeyBindAPI;

public class Scene {
    
    private static final UniqueIDManager idManager = new UniqueIDManager();
    
    private final int sceneID;
    private final KeyBindAPI keyBinds;
    
    public Scene() {
        this.sceneID = idManager.request();
        this.keyBinds = new KeyBind();
    }
    
    public int getSceneID() {
        return sceneID;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Scene scene = (Scene) o;
    
        return sceneID == scene.sceneID;
    }
    
    @Override
    public int hashCode() {
        return sceneID;
    }
    
}
