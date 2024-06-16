package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Acceleration;
import org.etieskrill.engine.entity.component.DirectionalForceComponent;
import org.etieskrill.engine.entity.component.DynamicCollider;
import org.etieskrill.engine.entity.component.WorldSpaceAABB;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.joml.Vector3f;

public class PlayerEntity extends Entity {

    private final Transform transform;
    private final Acceleration moveForce;
    private final DynamicCollider collider;

    public PlayerEntity(int id) {
        super(id);

        this.transform = new Transform();
        this.moveForce = new Acceleration(new Vector3f());
        this.collider = new DynamicCollider();

        addComponent(transform);
        addComponent(new AABB(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        addComponent(new WorldSpaceAABB());
        addComponent(collider);
        addComponent(new DirectionalForceComponent(new Vector3f(0, -15, 0)));
        addComponent(moveForce);
    }

    public Transform getTransform() {
        return transform;
    }

    public Acceleration getMoveForce() {
        return moveForce;
    }

    public DynamicCollider getCollider() {
        return collider;
    }

}
