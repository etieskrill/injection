package org.etieskrill.engine.entity.service.impl;

import lombok.AllArgsConstructor;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.particle.ParticleNode;
import org.etieskrill.engine.graphics.particle.ParticleRenderer;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class ParticleRenderService implements Service {

    private final ParticleRenderer renderer;
    private final Camera camera;

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, ParticleNode.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        ParticleNode node = targetEntity.getComponent(ParticleNode.class);
        node.update(delta);
        renderer.renderParticles(node, camera);
    }

    @Override
    public Set<Class<? extends Service>> runAfter() {
        return Set.of(RenderService.class);
    }

}
