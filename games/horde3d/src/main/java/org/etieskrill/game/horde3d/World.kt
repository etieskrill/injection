package org.etieskrill.game.horde3d

import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.DynamicCollider
import org.etieskrill.engine.entity.component.PointLightComponent
import org.etieskrill.engine.entity.component.StaticCollider
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.system.EntitySystem
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.framebuffer.PointShadowMapArray
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.model.ModelFactory
import org.etieskrill.engine.graphics.texture.AbstractTexture
import org.etieskrill.engine.graphics.texture.AbstractTexture.Type.*
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.Textures
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.timesAssign
import kotlin.random.Random

class World(
    entitySystem: EntitySystem
) {

    internal val sunLight: DirectionalLight
    internal val sunTransform: Transform
    internal lateinit var cubeTransform: Transform

    init {
        val floorModel = ModelFactory.box(Vector3f(100f, .1f, 100f))

        floorModel.nodes[2].meshes[0].material.apply {
            setProperty(Material.Property.SHININESS, 256f)
            textures.apply {
                clear()
                add(Textures.ofFile("textures/TilesSlateSquare001_COL_2K_METALNESS.png", DIFFUSE));
                add(Textures.ofFile("textures/TilesSlateSquare001_ROUGHNESS_2K_METALNESS.png", SPECULAR))
                add(
                    Texture2D.FileBuilder("textures/TilesSlateSquare001_NRM_2K_METALNESS.png", NORMAL)
                        .setFormat(AbstractTexture.Format.RGB) //TODO MMMMMMHHHHH select correct format automatically
                        .build()
                )
            }
        }

        entitySystem.createEntity {
            +Transform(Vector3f(0f, -1f, 0f))
            +floorModel.boundingBox
            +WorldSpaceAABB()
            +StaticCollider()
            +Drawable(floorModel, textureScale = Vector2f(15f))
        }

        val sphere = MODELS.load("sphere") {
            Model.Builder("Sphere.obj")
                .optimiseMeshes(2000, .05f)
                .build()
        }

        sunTransform = Transform(position = Vector3f(50f), scale = Vector3f(.35f))
        sunLight = DirectionalLight(Vector3f(-1f), Vector3f(1f), Vector3f(5f), Vector3f(5f))
        val sun = entitySystem.createEntity {
            +sunTransform
            +sunLight
            +Drawable(Model(sphere))
        }

        val light1 = PointLight(
            Vector3f(10f, 0f, 10f),
            Vector3f(2f, .3f, .25f), Vector3f(5f, 1.5f, 1f), Vector3f(5f, 1.5f, 1f),
            1f, .14f, .07f
        )
        val lightModel1 = Model(sphere)

        val light2 = PointLight(
            Vector3f(-10f, 0f, -10f),
            Vector3f(2f, .3f, .25f), Vector3f(5f, 1.5f, 1f), Vector3f(5f, 1.5f, 1f),
            1f, .14f, .07f
        )
        val lightModel2 = Model(sphere)

        MODELS.load("brick-cube") { Model.ofFile("brick-cube.obj") }
        val random = Random(69420)
        for (i in 0..10) {
            val transform = Transform(
                position = Vector3f(
                    random.nextFloat() * 30f - 15f,
                    random.nextFloat() * 3f,
                    random.nextFloat() * 30f - 15f
                ),
                rotation = Quaternionf().rotationAxis(
                    (random.nextFloat() * 2f * Math.PI - Math.PI).toFloat(),
                    Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat())
                        .mul(2f).sub(1f, 1f, 1f)
                        .normalize()
                ),
                scale = Vector3f(3f)
            )
            val cubeModel = MODELS.get("brick-cube")!!
            entitySystem.createEntity {
                +transform
                +Drawable(cubeModel)
                +cubeModel.boundingBox
                +WorldSpaceAABB()

                if (i == 1) {
                    +DynamicCollider(Vector3f(transform.position))
                } else {
                    +StaticCollider()
                }
            }

            if (i == 0) cubeTransform = transform
        }

        val directionalShadowMap = DirectionalShadowMap(Vector2i(2048))
        val pointShadowMaps = PointShadowMapArray(Vector2i(1024), 2)

        val sunLightCamera = OrthographicCamera(directionalShadowMap.size, 30f, -30f, -30f, 30f).apply {
            far = 40f
            position = Vector3f(10f, 20f, 10f)
            setRotation(-45f, 135f, 0f)
        }
        sun.withComponent(DirectionalLightComponent(sunLight, directionalShadowMap, sunLightCamera))

        val pointShadowNearPlane = .1f
        val pointShadowFarPlane = 40f

        entitySystem.createEntity {
            +Transform(Vector3f(light1.position)).apply { scale *= 0.01f }
            +Drawable(lightModel1)
            val pointLightCombined1 =
                pointShadowMaps.calculateCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light1)
            +PointLightComponent.withShadowMaps(light1, pointShadowMaps, 0, pointLightCombined1, pointShadowFarPlane)
        }

        entitySystem.createEntity {
            +Transform(Vector3f(light2.position)).apply { scale *= 0.01f }
            +Drawable(lightModel2)
            val pointLightCombined2 =
                pointShadowMaps.calculateCombinedMatrices(pointShadowNearPlane, pointShadowFarPlane, light2)
            +PointLightComponent.withShadowMaps(light2, pointShadowMaps, 1, pointLightCombined2, pointShadowFarPlane)
        }
    }

}
