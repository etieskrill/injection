package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.gl.shader.impl.AnimationShader;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.util.Loaders;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModelAnimations;
import static org.joml.Math.atan2;

public class PlayerEntity extends Entity {

    private final Transform transform;
    private final Acceleration moveForce;
    private final Animator animator;

    private float rotationSmooth;
    private float walkingTransition;

    public PlayerEntity(int id) {
        super(id);

        this.transform = new Transform();
        this.moveForce = new Acceleration(new Vector3f(), 2);

        addComponent(transform);

        addComponent(new AABB(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        addComponent(new WorldSpaceAABB());

        addComponent(new DynamicCollider());
        addComponent(new DirectionalForceComponent(new Vector3f(0, -15, 0)));

        addComponent(moveForce);
        addComponent(new Friction(8f));
        addComponent(new OnGround(5f, .15f));

        Model model = Loaders.ModelLoader.get().load("player", () ->
                new Model.Builder("mixamo_walk_forward_skinned_vampire.dae")
                        .disableCulling()
//                        .setInitialTransform(new Transform().setScale(0.01f))
                        .build());
        Drawable drawable = new Drawable(model);
        drawable.setShader(new AnimationShader());
        addComponent(drawable);

        animator = new Animator(model);
        animator.add(loadModelAnimations("mixamo_orc_idle.dae", model).getFirst());
        animator.add(loadModelAnimations("mixamo_walking_forward.dae", model).getFirst(),
                layer -> layer.playbackSpeed(1.2).weight(0));
        animator.play();
        addComponent(animator);

        addComponent(new Scripts(List.of(
                this::rotatePlayerToHeading,
                this::updateWalkingTransition
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

    public Transform getTransform() {
        return transform;
    }

    public Acceleration getMoveForce() {
        return moveForce;
    }

}
