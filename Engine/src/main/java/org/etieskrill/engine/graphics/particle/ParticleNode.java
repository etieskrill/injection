package org.etieskrill.engine.graphics.particle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.etieskrill.engine.entity.component.Transform;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * A particle node is a component for grouping {@link ParticleEmitter}s and creating particle group hierarchies.
 * Updates are automatically propagated throughout the hierarchy. Each node has a {@link Transform}, which is always in
 * respect to its parent node, except for the root node, which is in respect to the world it is placed in. A parent node
 * is always rendered before any of its children.
 */
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
public class ParticleNode {

    private final @Builder.Default Transform transform = new Transform();
    private final @Singular List<@NotNull ParticleNode> children;
    private final @Singular List<@NotNull ParticleEmitter> emitters;

    public void update(double delta) {
        emitters.forEach(emitter -> emitter.update(delta));
        children.forEach(child -> child.update(delta));
    }

}
