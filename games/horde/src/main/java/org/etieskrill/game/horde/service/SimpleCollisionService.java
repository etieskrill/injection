package org.etieskrill.game.horde.service;

import lombok.AllArgsConstructor;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.game.horde.component.Collider;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.BiConsumer;

@AllArgsConstructor
public class SimpleCollisionService implements Service {

    private final @Nullable Effect effect;

    public SimpleCollisionService() {
        this(null);
    }

    @FunctionalInterface
    public interface Effect extends BiConsumer<Entity, Entity> {
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, Collider.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        var transform = targetEntity.getComponent(Transform.class);
        var collider = targetEntity.getComponent(Collider.class);
        if (collider.isImmobile()) return;

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

            if (effect != null) {
                effect.accept(targetEntity, otherEntity);
                continue;
            }

            if (otherCollider.isSolid()) {
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
