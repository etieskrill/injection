package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.animation.NodeFilter;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.util.Loaders;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;

import static org.etieskrill.engine.graphics.animation.AnimationMixer.AnimationBlendMode.OVERRIDING;
import static org.etieskrill.engine.graphics.model.loader.Loader.loadModelAnimations;
import static org.joml.Math.atan2;

public class PlayerEntity extends Entity {

    private final Transform transform;
    private final DynamicCollider collider;
    private final Acceleration moveForce;
    private final VampireShader shader;
    private final Animator animator;
    private final DashState dashState;

    private float rotationSmooth;
    private float walkingTransition;

    public PlayerEntity(int id) {
        super(id);

        this.transform = new Transform();
        this.moveForce = new Acceleration(new Vector3f(), 20);

        addComponent(transform);

        addComponent(new AABB(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        addComponent(new WorldSpaceAABB());

        collider = new DynamicCollider();
        addComponent(collider);
        addComponent(new DirectionalForceComponent(new Vector3f(0, -15, 0)));

        addComponent(moveForce);
        addComponent(new Friction(8f));

        Model model = Loaders.ModelLoader.get().load("player", () ->
                new Model.Builder("vampire.glb")
                        .hasTransparency()
                        .optimiseMeshes()
                        .build());
        shader = new VampireShader();
        Drawable drawable = new Drawable(model, shader);
        addComponent(drawable);

        animator = new Animator(model);
        animator.add(loadModelAnimations("vampire_idle.glb", model).getFirst());
        animator.add(loadModelAnimations("vampire_walk.glb", model).getFirst(),
                layer -> layer.playbackSpeed(1.2).weight(0));
        animator.add(loadModelAnimations("vampire_run.glb", model).getFirst(), layer -> layer.setEnabled(false));
        animator.add(loadModelAnimations("vampire_melee.glb", model).getFirst(), layer -> layer
                .blendMode(OVERRIDING)
                .filter(NodeFilter.tree(model.getNodes().stream().filter(node -> node.getName().equals("mixamorig:Spine1")).findAny().get()))
                .playbackSpeed(1.5)
                .enabled(false));
        animator.play();
        addComponent(animator);

        dashState = new DashState(.6f, 5, 20, 100);
        addComponent(dashState);

        addComponent(new Scripts(List.of(
                this::rotatePlayerToHeading,
                this::updateWalkingTransition,
                this::updateDashAction
        )));
    }

    private void rotatePlayerToHeading(double delta) {
        if (moveForce.getForce().x() == 0 && moveForce.getForce().z() == 0) return;

        float playerRotation = atan2(moveForce.getForce().x(), moveForce.getForce().z());
        playerRotation = normalise(playerRotation);
        float diff = rotationSmooth - playerRotation;
        diff = normalise(diff);

        rotationSmooth -= 5f * (float) delta * diff;
        rotationSmooth = normalise(rotationSmooth);

        transform.applyRotation(quat -> quat.rotationY(rotationSmooth));
    }

    private float normalise(float angle) {
        if (angle >= Math.PI - 1e-5) return angle - 2 * (float) Math.PI;
        else if (angle <= -Math.PI + 1e-5) return angle + 2 * (float) Math.PI;
        else return angle;
    }

    private void updateWalkingTransition(double delta) {
        float walkingTransitionDelta = walkingTransition - (moveForce.getForce().equals(0, 0, 0) ? 0 : 1);
        walkingTransition -= 5 * (float) delta * walkingTransitionDelta;

        animator.getAnimationMixer().setWeight(0, 1 - walkingTransition);
        animator.getAnimationMixer().setWeight(1, walkingTransition);
    }

    private void updateDashAction(double delta) {
        dashState.update((float) delta);

        shader.setUniform("material.alpha", (float) (dashState.isActive() ? .1 : 1));
        moveForce.setFactor(dashState.isActive() ? dashState.getDashSpeed() : dashState.getRegularSpeed());
        collider.setStaticOnly(dashState.isActive());
    }

    public Transform getTransform() {
        return transform;
    }

    public Acceleration getMoveForce() {
        return moveForce;
    }

    public DashState getDashState() {
        return dashState;
    }

}