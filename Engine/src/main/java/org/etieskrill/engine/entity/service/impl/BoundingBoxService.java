package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.WorldSpaceAABB;
import org.etieskrill.engine.entity.service.Service;
import org.joml.primitives.AABBf;

import java.util.List;

public class BoundingBoxService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, AABBf.class, WorldSpaceAABB.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Transform transform = targetEntity.getComponent(Transform.class);
        AABBf aabb = targetEntity.getComponent(AABBf.class);
        WorldSpaceAABB worldSpaceAABB = targetEntity.getComponent(WorldSpaceAABB.class);

        aabb.transform(transform.getMatrix(), worldSpaceAABB);
    }

}
