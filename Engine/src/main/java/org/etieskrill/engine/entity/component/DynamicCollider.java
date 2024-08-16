package org.etieskrill.engine.entity.component;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DynamicCollider {

    private final Vector3f previousPosition;

    //TODO add collision groups or marker based filtering for collision pairs
    private boolean staticOnly = false;

    public DynamicCollider() {
        this(new Vector3f());
    }

    public DynamicCollider(Vector3f previousPosition) {
        this.previousPosition = previousPosition;
    }

    public Vector3f getPreviousPosition() {
        return previousPosition;
    }

    public void setPreviousPosition(Vector3fc previousPosition) {
        this.previousPosition.set(previousPosition);
    }

    public boolean isStaticOnly() {
        return staticOnly;
    }

    public void setStaticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
    }

}
