package org.etieskrill.game.horde;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.impl.AnimationService;
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.camera.PerspectiveCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.etieskrill.engine.graphics.gl.shader.impl.AnimationShader;
import org.etieskrill.engine.graphics.gl.shader.impl.PhongNoMaterialShader;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.model.loader.Loader;
import org.etieskrill.engine.input.controller.CursorCameraController;
import org.etieskrill.engine.input.controller.KeyCameraController;
import org.etieskrill.engine.util.Loaders;
import org.etieskrill.engine.util.Loaders.ModelLoader;
import org.etieskrill.engine.window.Window;
import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.joml.Math.*;

public class RuntimeMeshOptimisation extends GameApplication {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeMeshOptimisation.class);

    List<Transform> zombieTransforms;

    @Override
    protected void init() {
        PerspectiveCamera camera = new PerspectiveCamera(window.getSize().toVec());
        camera.setOrientation(0, -90, 0);

        entitySystem.addService(new AnimationService());
        entitySystem.addService(new DirectionalShadowMappingService(renderer));
//        entitySystem.addService(new PostProcessingRenderService(renderer, camera, window.getSize().toVec()));

        entitySystem.createEntity(id -> new Entity(id)
                .addComponent(new Transform().setPosition(new Vector3f(0, -1.5f, 0)))
                .addComponent(new Drawable(ModelFactory.box(new Vector3f(30, 1, 30)), new PhongNoMaterialShader()))
        );

        entitySystem.createEntity(id -> new Entity(id)
                .addComponent(new DirectionalLightComponent(new DirectionalLight(
                        new Vector3f(1, -1, -1), new Vector3f(1), new Vector3f(1), new Vector3f(1)),
                        DirectionalShadowMap.generate(new Vector2i(4096)),
                        new Matrix4f()
                                .ortho(-20, 20, -20, 20, .1f, 40)
                                .mul(new Matrix4f().lookAt(
                                        new Vector3f(10),
//                                        new Vector3f(.01f, 10, 0), //TODO looking perfectly down does not work for some reason, do some checks
                                        new Vector3f(0),
                                        new Vector3f(0, 1, 0)
                                ))
                )));

        zombieTransforms = new ArrayList<>();

        spawnZombies();
        spawnSkeletonZombies();

        window.addCursorInputs(new CursorCameraController(camera));
        window.getCursor().disable();
        window.addKeyInputs(new KeyCameraController(camera));

        window.setScene(new DebugInterface(window.getSize().toVec(), renderer, pacer));
    }

    private void spawnZombies() {
        final float numZombies = 100;
        Random random = new Random();
        for (int i = 0; i < numZombies; i++) {
            float angle = toRadians(((float) i / numZombies) * 360);

            Model model = ModelLoader.get().load("zombie", () ->
                    new Model.Builder("mixamo_zombie_skinned_walking.glb")
                            .optimiseMeshes()
                            .build());

            Animator animator = new Animator(model);
            animator.add(Loaders.AnimationLoader.get().load("mixamo_zombie_walking", () ->
                    Loader.loadModelAnimations("mixamo_zombie_walking.glb", model).getFirst()));
            animator.play(random.nextFloat() * .25f);

            var zombie = entitySystem.createEntity(id -> new Entity(id)
                    .addComponent(new Transform()
                            .setPosition(new Vector3f(8 * cos(angle), -1, 8 * sin(angle))))
                    .addComponent(new Drawable(model, new ZombieShader()))
                    .addComponent(animator));
            zombieTransforms.add(zombie.getComponent(Transform.class));
        }
    }

    private void spawnSkeletonZombies() {
        final float numSkellyZombies = 25;
        Random random = new Random();
        for (int i = 0; i < numSkellyZombies; i++) {
            float angle = toRadians(360 * i / numSkellyZombies);

            Model model = ModelLoader.get().load("skeleton_zombie", () ->
                    new Model.Builder("mixamo_skeletonzombie_skin.glb").optimiseMeshes().build());

            Animator animator = new Animator(model);
            animator.add(Loaders.AnimationLoader.get().load("silly_dancing",
                            Loader.loadModelAnimations("mixamo_bboy_hip_hop.fbx", model, (modelBone, animBone) ->
                                    animBone.contains(modelBone.substring(modelBone.lastIndexOf(':'))))::getFirst),
                    layer -> layer.setPlaybackSpeed(.8f));
            animator.play(random.nextFloat() * .175f);

            entitySystem.createEntity(id -> new Entity(id)
                    .addComponent(new Transform()
                            .setPosition(new Vector3f(8 * cos(angle), 1, 8 * sin(angle)))
                            .applyRotation(quat -> quat.rotateY(-angle - toRadians(90))))
                    .addComponent(new Drawable(model, new AnimationShader()))
                    .addComponent(animator)
            );
        }

        renderer.setQueryGpuTime(false);
    }

    @Override
    protected void loop(double delta) {
        for (int i = 0; i < zombieTransforms.size(); i++) {
            float angle = toRadians(360 * i / (float) zombieTransforms.size());
            float anglePosition = angle + (float) ((.05 * pacer.getTime()) % (2 * Math.PI));
            zombieTransforms.get(i)
                    .setPosition(new Vector3f(8 * cos(anglePosition), -1, 8 * sin(anglePosition)))
                    .applyRotation(quat -> quat.rotationY(-anglePosition));
        }
    }

    public RuntimeMeshOptimisation() {
        super(60, new Window.Builder()
                .setVSyncEnabled(true)
                .setMode(Window.WindowMode.BORDERLESS)
                .build());
    }

    public static void main(String[] args) {
        new RuntimeMeshOptimisation();
    }
}
