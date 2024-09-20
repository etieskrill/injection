package org.etieskrill.game.horde3d;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.*;
import org.etieskrill.engine.entity.system.EntitySystem;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.camera.OrthographicCamera;
import org.etieskrill.engine.graphics.data.DirectionalLight;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap;
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray;
import org.etieskrill.engine.graphics.model.Material;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.model.ModelFactory;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.primitives.AABBf;

import java.util.Random;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*;
import static org.etieskrill.game.horde3d.EntityApplication.MODELS;

public class World {

    private DirectionalLight sunLight;
    private Transform sunTransform;
    private Transform cubeTransform;

    public World(EntitySystem entitySystem) {
        init(entitySystem);
    }

    private void init(EntitySystem entitySystem) {
        Model floorModel = ModelFactory.box(new Vector3f(100, .1f, 100));
        Material floorMaterial = floorModel.getNodes().get(2).getMeshes().getFirst().getMaterial();
        floorMaterial.setProperty(Material.Property.SHININESS, 256f);
        floorMaterial.getTextures().clear();
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_COL_2K_METALNESS.png", DIFFUSE));
        floorMaterial.getTextures().add(Textures.ofFile("TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", SPECULAR));
        floorMaterial.getTextures().add(
                new Texture2D.FileBuilder("TilesSlateSquare001_NRM_2K_METALNESS.png", NORMAL)
                        .setFormat(AbstractTexture.Format.RGB) //TODO MMMMMMHHHHH select correct format automatically
                        .build()
        );

        Entity floor = entitySystem.createEntity();
        Drawable floorDrawable = new Drawable(floorModel);
        floorDrawable.setTextureScale(new Vector2f(15));
        floor.withComponent(floorDrawable);

        floor.withComponent(floorModel.getTransform().setPosition(new Vector3f(0, -1, 0)));

        var floorAABB = floorModel.getBoundingBox();
        floor.withComponent(new AABBf(
                new Vector3f(floorAABB.minX(), floorAABB.minY(), floorAABB.minZ()).mul(floorModel.getInitialTransform().getScale()),
                new Vector3f(floorAABB.maxX(), floorAABB.maxY(), floorAABB.maxZ()).mul(floorModel.getInitialTransform().getScale())
        ));
        floor.withComponent(new WorldSpaceAABB());

        floor.withComponent(new StaticCollider());

        Model sphere = MODELS.load("sphere", () ->
                new Model.Builder("Sphere.obj")
                        .optimiseMeshes(5000, .05f)
                        .build());

        Entity sun = entitySystem.createEntity();
        sunLight = new DirectionalLight(new Vector3f(-1), new Vector3f(1f), new Vector3f(5), new Vector3f(5));
        sun.withComponent(sunLight);
        Model sunModel = new Model(sphere);
        sunModel.getTransform().setPosition(new Vector3f(50)).setScale(new Vector3f(.35f));
        sun.withComponent(new Drawable(sunModel));
        sunTransform = new Transform(sunModel.getTransform());
        sun.withComponent(sunTransform);

        PointLight light1 = new PointLight(new Vector3f(10, 0, 10),
                new Vector3f(2, .3f, .25f), new Vector3f(5, 1.5f, 1), new Vector3f(5, 1.5f, 1),
                1, .14f, .07f);
        Model lightModel1 = new Model(sphere);
        lightModel1.getTransform().setPosition(light1.getPosition()).setScale(.01f);

        PointLight light2 = new PointLight(new Vector3f(-10, 0, -10),
                new Vector3f(2, .3f, .25f), new Vector3f(5, 1.5f, 1), new Vector3f(5, 1.5f, 1),
                1, .14f, .07f);
        Model lightModel2 = new Model(sphere);
        lightModel2.getTransform().setPosition(light2.getPosition()).setScale(.01f);

        MODELS.load("brick-cube", () -> Model.ofFile("brick-cube.obj"));
        Random random = new Random(69420);
        for (int i = 0; i < 10; i++) {
            Model cubeModel = MODELS.get("brick-cube");
            cubeModel.getTransform()
                    .setPosition(new Vector3f(random.nextFloat() * 30 - 15, random.nextFloat() * 3, random.nextFloat() * 30 - 15))
                    .applyRotation(quat -> quat.rotationAxis((float) (random.nextFloat() * 2 * Math.PI - Math.PI),
                            new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
                                    .mul(2).sub(1, 1, 1)
                                    .normalize()))
                    .setScale(3);
            Entity cube = entitySystem.createEntity();
            cube.withComponent(new Drawable(cubeModel));
            cube.withComponent(cubeModel.getTransform());
            cube.withComponent(cubeModel.getBoundingBox());
            cube.withComponent(new WorldSpaceAABB());
            if (i == 1) {
                cube.withComponent(new DynamicCollider(new Vector3f(cubeModel.getTransform().getPosition())));
            } else {
                cube.withComponent(new StaticCollider());
            }

            if (i == 0) cubeTransform = cubeModel.getTransform();
        }

        DirectionalShadowMap directionalShadowMap = DirectionalShadowMap.generate(new Vector2i(2048));
        PointShadowMapArray pointShadowMaps = PointShadowMapArray.generate(new Vector2i(1024), 2);

        Camera sunLightCamera = new OrthographicCamera(directionalShadowMap.getSize(), 30, -30, -30, 30)
                .setFar(40)
                .setPosition(new Vector3f(10, 20, 10))
                .setRotation(-45, 135, 0);
        sun.withComponent(new DirectionalLightComponent(sunLight, directionalShadowMap, sunLightCamera));

        final float pointShadowNearPlane = .1f;
        final float pointShadowFarPlane = 40;

        Entity pointLight1 = entitySystem.createEntity();
        pointLight1.withComponent(lightModel1.getTransform());
        pointLight1.withComponent(new Drawable(lightModel1));
        Matrix4fc[] pointLightCombined1 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light1);
        pointLight1.withComponent(new PointLightComponent(light1, pointShadowMaps, 0, pointLightCombined1, pointShadowFarPlane));

        Entity pointLight2 = entitySystem.createEntity();
        pointLight2.withComponent(lightModel2.getTransform());
        pointLight2.withComponent(new Drawable(lightModel2));
        Matrix4fc[] pointLightCombined2 = pointShadowMaps.getCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light2);
        pointLight2.withComponent(new PointLightComponent(light2, pointShadowMaps, 1, pointLightCombined2, pointShadowFarPlane));
    }

    public DirectionalLight getSunLight() {
        return sunLight;
    }

    public Transform getSunTransform() {
        return sunTransform;
    }

    public Transform getCubeTransform() {
        return cubeTransform;
    }

}
