package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.entity.data.AABB;
import org.joml.Vector3f;

public class WorldSpaceAABB extends AABB {
    public WorldSpaceAABB() {
        super(new Vector3f(), new Vector3f());
    }
}
