package org.etieskrill.game.horde.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.game.horde.component.Collider;
import org.joml.Vector3f;

import java.util.List;

public class SimpleCollisionService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, Collider.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        var transform = targetEntity.getComponent(Transform.class);
        var collider = targetEntity.getComponent(Collider.class);

        for (Entity otherEntity : entities) {
            var otherTransform = otherEntity.getComponent(Transform.class);
            if (otherTransform == null) continue;
            var otherCollider = otherEntity.getComponent(Collider.class);
            if (otherCollider == null) continue;

            Vector3f dir = new Vector3f(otherTransform.getPosition()).sub(transform.getPosition());
            if (dir.equals(0, 0, 0))
                continue; //TODO this stupid fucking NaN edgecase is driving me up the wall - this should just be 0 for length anyway
            dir.y = 0;
            float overlap = (collider.getRadius() + otherCollider.getRadius()) - dir.length();
            if (overlap <= 0) continue;

            collider.getOnCollide().accept(targetEntity, otherEntity);

            if (!collider.isImmobile() && otherCollider.isSolid()) {
                if (otherCollider.isImmobile()) {
                    transform.translate(dir.normalize().mul(-overlap));
                } else {
                    transform.translate(dir.normalize().mul(-overlap / 2));
                    otherTransform.translate(dir.normalize().mul(overlap / 2));
                }
            }
        }
    }

}
