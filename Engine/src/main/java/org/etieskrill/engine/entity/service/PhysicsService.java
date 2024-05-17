package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalForceComponent;
import org.etieskrill.engine.entity.component.DynamicCollider;
import org.etieskrill.engine.entity.component.StaticCollider;
import org.etieskrill.engine.entity.component.WorldSpaceAABB;
import org.etieskrill.engine.entity.data.Transform;
import org.joml.Vector3f;

import java.util.List;

public class PhysicsService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, DynamicCollider.class, WorldSpaceAABB.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Transform transform = targetEntity.getComponent(Transform.class);
        DynamicCollider collider = targetEntity.getComponent(DynamicCollider.class);
        DirectionalForceComponent force = targetEntity.getComponent(DirectionalForceComponent.class);
        WorldSpaceAABB bb = targetEntity.getComponent(WorldSpaceAABB.class);

        Vector3f newPosition = new Vector3f();
        newPosition
                .add(transform.getPosition()).add(transform.getPosition())
                .sub(collider.getPreviousPosition())
                .add(new Vector3f(force.getForce()).mul((float) (delta * delta)));

        collider.setPreviousPosition(transform.getPosition());
        transform.setPosition(newPosition);

        for (Entity entity : entities) {
            DynamicCollider dynamicCollider = entity.getComponent(DynamicCollider.class);
            StaticCollider staticCollider = entity.getComponent(StaticCollider.class);
            if (entity == targetEntity
                    || (dynamicCollider == null && staticCollider == null)) {
                continue;
            }

            WorldSpaceAABB otherBB = entity.getComponent(WorldSpaceAABB.class);
            Transform otherTransform = entity.getComponent(Transform.class);
            if (otherBB == null || otherTransform == null) continue;
            if (!bb.overlapsWith(otherBB)) {
                continue;
            }

            if (bb.getMin().y() < otherBB.getMax().y()) {
                float diff = bb.getMin().y() - otherBB.getMax().y();

                if (dynamicCollider != null) { //TODO either only cancel motion orthogonal to correcting surface, or use acceleration
                    transform.getPosition().add(0, -diff / 2, 0);
                    collider.setPreviousPosition(transform.getPosition());
                    otherTransform.getPosition().add(0, diff / 2, 0);
                    dynamicCollider.setPreviousPosition(otherTransform.getPosition());
                } else {
                    transform.getPosition().add(0, -diff, 0);
                    collider.setPreviousPosition(transform.getPosition());
                }
            }
        }
    }

}
