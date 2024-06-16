package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Acceleration;
import org.etieskrill.engine.entity.component.OnGround;
import org.etieskrill.engine.entity.component.Transform;

import java.util.List;
import java.util.Set;

public class JumpService implements Service {
    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Acceleration.class, OnGround.class, Transform.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Acceleration acceleration = targetEntity.getComponent(Acceleration.class);
        Transform transform = targetEntity.getComponent(Transform.class);
        OnGround onGround = targetEntity.getComponent(OnGround.class);

        float verticalAcceleration = acceleration.getForce().y();
        if (verticalAcceleration > 0) {
            if (onGround.isOnGround()) {
                transform.getPosition().add(0, onGround.getJumpStrength(), 0);
                onGround.setOnGround(false);
                float bumpX = acceleration.getForce().x();
                float bumpZ = acceleration.getForce().z();
                float bumpStrength = .005f;
                transform.getPosition().add(bumpX * bumpStrength, 0, bumpZ * bumpStrength);
            }
        }
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(PhysicsService.class);
    }
}
