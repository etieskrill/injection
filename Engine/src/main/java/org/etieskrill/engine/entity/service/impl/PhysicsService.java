package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.service.Service;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class PhysicsService implements Service {

    private final NarrowCollisionSolver solver;
    private double lastDelta = 0;
    private boolean firstCall = true;

    public PhysicsService(@NotNull NarrowCollisionSolver solver) {
        this.solver = solver;
    }

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Transform.class, DynamicCollider.class, WorldSpaceAABB.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        if (firstCall) {
            lastDelta = delta;
            firstCall = false;
        }

        Transform transform = targetEntity.getComponent(Transform.class);
        DynamicCollider collider = targetEntity.getComponent(DynamicCollider.class);
        OnGround onGround = targetEntity.getComponent(OnGround.class);

        updatePosition(targetEntity, transform, collider, onGround, delta);

        solveCollisions(entities, targetEntity, transform, collider, onGround);
    }

    private void updatePosition(Entity targetEntity,
                                Transform transform,
                                DynamicCollider collider,
                                OnGround onGround,
                                double delta) {
        Vector3f velocity = new Vector3f(transform.getPosition())
                .sub(collider.getPreviousPosition());

        Friction friction = targetEntity.getComponent(Friction.class);
        if (friction != null) {
            float verticalVelocity = velocity.y();
            velocity.mul((float) (1f - (friction.getCoefficient() * delta)));
            velocity.y = verticalVelocity;
        }

        if (lastDelta > 0) velocity.mul((float) (delta / lastDelta));

        Vector3f newPosition = velocity.add(transform.getPosition());

        float correctedAccelDelta = (float) (delta * ((delta + lastDelta) / 2));

        DirectionalForceComponent force = targetEntity.getComponent(DirectionalForceComponent.class);
        if (force != null) {
            newPosition.add(new Vector3f(force.getForce()).mul(correctedAccelDelta));
        }

        Acceleration acceleration = targetEntity.getComponent(Acceleration.class);
        if (acceleration != null) {
            if (onGround != null && !onGround.isOnGround()) {
                acceleration.getForce().y = 0;
            }
            newPosition.add(new Vector3f(acceleration.getForce()).mul(acceleration.getFactor() * correctedAccelDelta));
        }

        lastDelta = delta;
        collider.setPreviousPosition(transform.getPosition());
        transform.setPosition(newPosition);
    }

    private void solveCollisions(List<Entity> entities,
                                 Entity targetEntity,
                                 Transform transform,
                                 DynamicCollider collider,
                                 OnGround onGround) {
        WorldSpaceAABB bb = targetEntity.getComponent(WorldSpaceAABB.class);

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
                solver.solveStatic(transform, otherTransform, bb, otherBB, collider, staticCollider, onGround);
            }
        }
    }

    public interface NarrowCollisionSolver {

        void solveStatic(Transform transform,
                         Transform otherTransform,
                         WorldSpaceAABB bb,
                         WorldSpaceAABB otherBB,
                         DynamicCollider collider,
                         StaticCollider otherCollider,
                         OnGround onGround);

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
                                    StaticCollider otherCollider,
                                    OnGround onGround) {
                Vector3f overlap = new Vector3f(bb.getMax()).min(otherBB.getMax())
                        .sub(new Vector3f(bb.getMin()).max(otherBB.getMin()));

                int component = overlap.minComponent();
                float overlapComponent = overlap.get(component);
                if (overlapComponent <= 0) {
                    return;
                }
                if (bb.getCenter().get(component) < otherBB.getCenter().get(component)) {
                    overlapComponent = -overlapComponent;
                }
                if (component == 1 && onGround != null) {
                    onGround.setOnGround(true);
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
                if (collider.isStaticOnly() || otherCollider.isStaticOnly()) return;

                Vector3f overlap = new Vector3f(bb.getMax()).min(otherBB.getMax())
                        .sub(new Vector3f(bb.getMin()).max(otherBB.getMin()));

                int component = overlap.minComponent();
                float overlapComponent = overlap.get(component);
                if (overlapComponent <= 0)
                    return;
                if (bb.getCenter().get(component) < otherBB.getCenter().get(component)) {
                    overlapComponent = -overlapComponent;
                }
                transform.getPosition().setComponent(component,
                        transform.getPosition().get(component) + overlapComponent / 2);
                otherTransform.getPosition().setComponent(component,
                        otherTransform.getPosition().get(component) - overlapComponent / 2);
//                TODO introduce simple elasticity - multiply previous position correction with factor
//                collider.getPreviousPosition().setComponent(component, transform.getPosition().get(component));
//                otherCollider.getPreviousPosition().setComponent(component, otherTransform.getPosition().get(component));
            }
        };

    }

}
