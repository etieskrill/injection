package org.etieskrill.engine.entity.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Getter
@RequiredArgsConstructor
public class DynamicCollider {

    private final Vector3f previousPosition;

    //TODO add collision groups or marker based filtering for collision pairs
    private @Setter boolean staticOnly = false;

    public DynamicCollider() {
        this(new Vector3f());
    }

    public void setPreviousPosition(Vector3fc previousPosition) {
        this.previousPosition.set(previousPosition);
    }

}
