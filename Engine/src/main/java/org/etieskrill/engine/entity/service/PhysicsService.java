package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalForceComponent;
import org.etieskrill.engine.entity.data.Transform;
import org.joml.Vector3f;

import java.util.List;

public class PhysicsService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, DirectionalForceComponent.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Transform transform = targetEntity.getComponent(Transform.class);
        DirectionalForceComponent force = targetEntity.getComponent(DirectionalForceComponent.class);

        transform.getPosition().add(force.getForce().mul((float) delta, new Vector3f()));
    }

}
