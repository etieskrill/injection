package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.data.Transform;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class PhysicsService implements Service {

    private final NarrowCollisionSolver solver;

    public PhysicsService(@NotNull NarrowCollisionSolver solver) {
        this.solver = solver;
    }

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
                .sub(collider.getPreviousPosition());

        if (force != null) {
            newPosition.add(new Vector3f(force.getForce()).mul((float) (delta * delta)));
        }

        Acceleration acceleration = targetEntity.getComponent(Acceleration.class);
        if (acceleration != null) {
            newPosition.add(new Vector3f(acceleration.getForce()).mul((float) (delta * delta)));
        }

        collider.setPreviousPosition(transform.getPosition());
        transform.setPosition(newPosition);

        for (Entity entity : entities) {
            if (entity == targetEntity) {
                continue;
            }

            DynamicCollider dynamicCollider = entity.getComponent(DynamicCollider.class);
            StaticCollider staticCollider = entity.getComponent(StaticCollider.class);
            if (dynamicCollider == null && staticCollider == null) {
                continue;
            }

            WorldSpaceAABB otherBB = entity.getComponent(WorldSpaceAABB.class);
            Transform otherTransform = entity.getComponent(Transform.class);
            if (otherBB == null || otherTransform == null) continue;
            if (!bb.overlapsWith(otherBB)) {
                continue;
            }

            if (dynamicCollider != null) {
                solver.solveDynamic(transform, otherTransform, bb, otherBB, collider, dynamicCollider);
            } else {
                solver.solveStatic(transform, otherTransform, bb, otherBB, collider, staticCollider);
            }
        }
    }

    public interface NarrowCollisionSolver {

        void solveStatic(Transform transform,
                         Transform otherTransform,
                         WorldSpaceAABB bb,
                         WorldSpaceAABB otherBB,
                         DynamicCollider collider,
                         StaticCollider otherCollider);

        void solveDynamic(Transform transform,
                          Transform otherTransform,
                          WorldSpaceAABB bb,
                          WorldSpaceAABB otherBB,
                          DynamicCollider collider,
                          DynamicCollider otherCollider);

        NarrowCollisionSolver AABB_SOLVER = new NarrowCollisionSolver() {
            @Override
            public void solveStatic(Transform transform,
                                    Transform otherTransform,
                                    WorldSpaceAABB bb,
                                    WorldSpaceAABB otherBB,
                                    DynamicCollider collider,
                                    StaticCollider otherCollider) {
                Vector3f overlap = new Vector3f(bb.getMax()).min(otherBB.getMax())
                        .sub(new Vector3f(bb.getMin()).max(otherBB.getMin()));

                int component = overlap.minComponent();
                float overlapComponent = overlap.get(component);
                if (bb.getCenter().get(component) < otherBB.getCenter().get(component)) {
                    overlapComponent = -overlapComponent;
                }
                transform.getPosition().setComponent(component,
                        transform.getPosition().get(component) + overlapComponent);
                collider.getPreviousPosition().setComponent(component, transform.getPosition().get(component));
            }

            @Override
            public void solveDynamic(Transform transform,
                                     Transform otherTransform,
                                     WorldSpaceAABB bb,
                                     WorldSpaceAABB otherBB,
                                     DynamicCollider collider,
                                     DynamicCollider otherCollider) {
                //TODO
            }
        };

    }

}
