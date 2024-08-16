package org.etieskrill.game.horde;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.loader.Loader;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.Loaders.AnimationLoader;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

import static org.joml.Math.atan2;

public class Zombie extends Entity {

    private final Transform transform;
    private final DynamicCollider collider;
    private final Acceleration acceleration;

    private float rotationSmooth;

    public Zombie(int id) {
        super(id);

        transform = new Transform();
        addComponent(transform);
        addComponent(new AABB(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        addComponent(new WorldSpaceAABB());

        Model model = Loaders.ModelLoader.get().load("zombie", () ->
                new Model.Builder("mixamo_zombie_skinned_walking.glb")
                        .setName("zombie")
//                        .optimiseMeshes()
                        .build());
//        addComponent(new Drawable(model, ShaderLoader.get().load("animation_shader", AnimationShader::new))); //FIXME
        addComponent(new Drawable(model, new ZombieShader()));

        Animator animator = new Animator(model);
        Animation walkingAnimation = AnimationLoader.get().load("zombie_walking", () ->
                Loader.loadModelAnimations("mixamo_zombie_walking.glb", model).getFirst());
        animator.add(walkingAnimation, layer -> layer.setPlaybackSpeed(2));

        double offset = new Random().nextDouble(0, walkingAnimation.getDurationSeconds());
        animator.play(offset);
        addComponent(animator);

        addComponent(new DirectionalForceComponent(new Vector3f(0, -15, 0)));
        addComponent(new Friction(8));
        collider = new DynamicCollider();
        collider.setPreviousPosition(transform.getPosition());
        addComponent(collider);

        acceleration = new Acceleration(new Vector3f(), 20);
        addComponent(acceleration);

        addComponent(new Scripts(List.of(
                this::rotateToHeading
        )));
    }

    private void rotateToHeading(double delta) {
        if (acceleration.getForce().x() == 0 && acceleration.getForce().z() == 0) return;

        float playerRotation = atan2(acceleration.getForce().x(), acceleration.getForce().z());
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

    public Transform getTransform() {
        return transform;
    }

    public DynamicCollider getCollider() {
        return collider;
    }

    public Acceleration getAcceleration() {
        return acceleration;
    }

}
