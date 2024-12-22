package org.etieskrill.game.horde.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.etieskrill.engine.entity.Entity;

import java.util.function.BiConsumer;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class Collider {
    private final float radius;
    private boolean immobile = false;
    private boolean solid = true;
    private BiConsumer<Entity, Entity> onCollide = (entity, otherEntity) -> {
    };

    public Collider(float radius, boolean immobile, boolean solid) {
        this(radius, immobile, solid, (entity, otherEntity) -> {
        });
    }
}
