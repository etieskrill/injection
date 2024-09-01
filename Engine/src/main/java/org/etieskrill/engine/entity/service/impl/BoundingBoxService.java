package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.AABB;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.entity.component.WorldSpaceAABB;
import org.etieskrill.engine.entity.service.Service;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.List;

public class BoundingBoxService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, AABB.class, WorldSpaceAABB.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Transform transform = targetEntity.getComponent(Transform.class);
        AABB aabb = targetEntity.getComponent(AABB.class);
        WorldSpaceAABB worldSpaceAABB = targetEntity.getComponent(WorldSpaceAABB.class);

        transformBoundingBox(transform, aabb, worldSpaceAABB);
    }

    private void transformBoundingBox(TransformC transform, AABB boundingBox, WorldSpaceAABB worldSpaceBoundingBox) {
        Matrix4fc transformMatrix = transform.getMatrix();

        Vector3f min = (Vector3f) worldSpaceBoundingBox.getMin();
        Vector3f max = (Vector3f) worldSpaceBoundingBox.getMax();

        transformMatrix.transformAab(boundingBox.getMin(), boundingBox.getMax(), min, max);

        worldSpaceBoundingBox.set(min, max);
    }

}
