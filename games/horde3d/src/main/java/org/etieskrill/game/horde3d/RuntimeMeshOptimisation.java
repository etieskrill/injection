package org.etieskrill.game.horde3d;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.DirectionalLightComponent;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.service.impl.AnimationService;
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService;
import org.etieskrill.engine.entity.service.impl.RenderService;
import org.etieskrill.engine.graphics.animation.Animator;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
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
import org.joml.Random;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.joml.Math.*;

public class RuntimeMeshOptimisation extends GameApplication {

    List<Transform> zombieTransforms;

    @Override
    protected void init() {
        PerspectiveCamera camera = new PerspectiveCamera(window.getSize().getVec());
        camera.setRotation(0, -90, 0);

        entitySystem.addService(new AnimationService());
        entitySystem.addService(new DirectionalShadowMappingService(renderer));
        entitySystem.addService(new RenderService(renderer, camera, window.getSize().getVec()));

        entitySystem.createEntity(id -> new Entity(id)
                .withComponent(new Transform().setPosition(new Vector3f(0, -1.5f, 0)))
                .withComponent(new Drawable(ModelFactory.box(new Vector3f(30, 1, 30)), new PhongNoMaterialShader()))
        );

        entitySystem.createEntity(id -> new Entity(id)
                .withComponent(new DirectionalLightComponent(new DirectionalLight(
                        new Vector3f(1, -1, -1), new Vector3f(1), new Vector3f(1), new Vector3f(1)),
                        DirectionalShadowMap.generate(new Vector2i(4096)),
                        new OrthographicCamera(new Vector2i(4096), 20, -20, -20, 20)
                                .setPosition(new Vector3f(10))
                                .setRotation(-45, 215, 0)
                                .setFar(40)
                )));

        zombieTransforms = new ArrayList<>();

        spawnZombies();
        spawnSkeletonZombies();

        window.addCursorInputs(new CursorCameraController(camera));
        window.getCursor().disable();
        window.addKeyInputs(new KeyCameraController(camera));

        window.setScene(new DebugInterface(window.getSize().getVec(), renderer, pacer));
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
                    .withComponent(new Transform()
                            .setPosition(new Vector3f(8 * cos(angle), -1, 8 * sin(angle))))
                    .withComponent(new Drawable(model, new ZombieShader()))
                    .withComponent(animator));
            zombieTransforms.add(zombie.getComponent(Transform.class));
        }
    }

    private void spawnSkeletonZombies() {
        final float numSkellyZombies = 25;
        Random random = new Random();
        for (int i = 0; i < numSkellyZombies; i++) {
            float angle = toRadians(360 * i / numSkellyZombies);

            Model model = ModelLoader.get().load("skeleton_zombie", () ->
                    new Model.Builder("mixamo_skeletonzombie_skin.glb")
                            .optimiseMeshes()
                            .build());

            Animator animator = new Animator(model);
            animator.add(Loaders.AnimationLoader.get().load("silly_dancing",
                            Loader.loadModelAnimations("mixamo_bboy_hip_hop.fbx", model, (modelBone, animBone) ->
                                    animBone.contains(modelBone.substring(modelBone.lastIndexOf(':'))))::getFirst),
                    layer -> layer.setPlaybackSpeed(.8f));
            animator.play(random.nextFloat() * .175f);

            entitySystem.createEntity(id -> new Entity(id)
                    .withComponent(new Transform()
                            .setPosition(new Vector3f(8 * cos(angle), 1, 8 * sin(angle)))
                            .applyRotation(quat -> quat.rotateY(-angle - toRadians(90))))
                    .withComponent(new Drawable(model, new AnimationShader()))
                    .withComponent(animator)
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
        super(Window.builder()
                .setVSyncEnabled(true)
                .setMode(Window.WindowMode.BORDERLESS)
                .build());
    }

    public static void main(String[] args) {
        new RuntimeMeshOptimisation();
    }
}
