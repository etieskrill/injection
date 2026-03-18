package org.etieskrill.game.horde3d;

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
import org.joml.primitives.AABBf;

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
        addComponentNoCheck(transform);
        addComponentNoCheck(new AABBf(new Vector3f(-.5f, 0, -.5f), new Vector3f(.5f, 2, .5f)));
        addComponentNoCheck(new WorldSpaceAABB());

        Model model = Loaders.ModelLoader.get().load("zombie", () ->
                new Model.Builder("mixamo_zombie_skinned_walking.glb")
                        .setName("zombie")
                        .optimiseMeshes(5000, 0.01f)
                        .build());
//        addComponentNoCheck(new Drawable(model, ShaderLoader.get().load("animation_shader", AnimationShader::new))); //FIXME
        addComponentNoCheck(new Drawable(model, new ZombieShader()));

        Animator animator = new Animator(model);
        Animation walkingAnimation = AnimationLoader.get().load("zombie_walking", () ->
                Loader.loadModelAnimations("mixamo_zombie_walking.glb", model).getFirst());
        animator.add(walkingAnimation, layer -> layer.setPlaybackSpeed(2));

        double offset = new Random().nextDouble(0, walkingAnimation.getDurationSeconds());
        animator.play(offset);
        addComponentNoCheck(animator);

        addComponentNoCheck(new DirectionalForceComponent(new Vector3f(0, -15, 0)));
        addComponentNoCheck(new Friction(8));
        collider = new DynamicCollider();
        collider.setPreviousPosition(transform.getPosition());
        addComponentNoCheck(collider);

        acceleration = new Acceleration(new Vector3f(), 20);
        addComponentNoCheck(acceleration);

        addComponentNoCheck(new Scripts(List.of(
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
