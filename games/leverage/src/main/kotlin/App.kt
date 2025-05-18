package io.github.etieskrill.games.leverage

import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.component.DirectionalLightComponent
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.DirectionalShadowMappingService
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.framebuffer.DirectionalShadowMap
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.gl.shader.UniformMappable
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.gl.shader.impl.StaticShader
import org.etieskrill.engine.graphics.gl.shader.impl.globalLights
import org.etieskrill.engine.graphics.gl.shader.impl.lights
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.glBindVertexArray
import org.lwjgl.opengl.GL30C.glGenVertexArrays

fun main() {
    App().run()
}

class App : GameApplication(window {
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
}) {

    private lateinit var shadowMap: DirectionalShadowMap
    private val blitShader = BlitShader()

    override fun init() {
        val shipModel = Loaders.ModelLoader.get().load("human-bb") { Model.ofFile("hooman-bb.glb") }

        shipModel.nodes.forEach { println(it.name) } //TODO some ootils for searching and traversing in node hierarchies would be nice
        val centralEnginePlume = shipModel.nodes.single { it.name == "engine-plume-main" }
        val farLeftEnginePlume = shipModel.nodes.single { it.name == "engine-plume-far-left" }

        //FIXME why is there no diffuse lighting? - try setting a material at least
        //TODO self-shadows would be nice - scratch that, they're a must
        val shader = StaticShader()
        shader.globalLights = arrayOf(DirectionalLight(Vector3f(-1f)))
        shader.lights = arrayOf( // @formatter:off
            PointLight(centralEnginePlume.hierarchyTransform.position, Vector3f(0f, 1f, 1f), Vector3f(0f, 1f, 1f), Vector3f(0f, 1f, 1f), 1f, .14f, .07f),
            PointLight(farLeftEnginePlume.hierarchyTransform.position, Vector3f(0f, 1f, 1f), Vector3f(0f, 1f, 1f), Vector3f(0f, 1f, 1f), 1f, .14f, .07f)
        ) // @formatter:on
        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(Drawable(shipModel, shader))

        val camera = PerspectiveCamera(window.currentSize).apply {
            setOrbit(true)
            setOrbitDistance(10f)
            setFar(50f)
        }

        shadowMap = DirectionalShadowMap.generate(Vector2i(1024))

        entitySystem.createEntity()
            .withComponent(Transform())
            .withComponent(
                DirectionalLightComponent(
                DirectionalLight(Vector3f(-1f)),
                shadowMap,
                OrthographicCamera(Vector2i(1024), 20f, -20f, -20f, 20f).apply {
                    // FIXME i hate myself
//                    setOrbit(true)
//                    setOrbitDistance(10f)
//                    setRotation(0f, 0f, 0f)
                    setPosition(Vector3f(10f))
                    setRotation(-45f, 0f, 0f)
                }
            ))

        entitySystem.addService(RenderService(renderer, camera, window.currentSize))
        entitySystem.addService(DirectionalShadowMappingService(renderer))

        window.addKeyInputs(KeyCameraController(camera))
        window.addCursorInputs(CursorCameraController(camera))

        window.cursor.disable()
    }

    override fun loop(delta: Double) {}

    private val dummyVAO = glGenVertexArrays()

    override fun render() {
        blitShader.size = Vector2f(300f)
        blitShader.sprite = shadowMap.texture
        blitShader.windowSize = Vector2f(window.currentSize)
        blitShader.start()

        glBindVertexArray(dummyVAO)
        glViewport(0, 0, window.currentSize.x(), window.currentSize.y()) //TODO i hate global state
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }
}

data class PhongMaterial(
//    var diffuse0: sampler2D?,
//    var specularTexture: bool,
//    var specular0: sampler2D,
//    var shininess: float,
//    var specularity: float,
//    var hasNormalMap: bool,
//    var normal0: sampler2D,
//    var emissiveTexture: bool,
//    var emissive0: sampler2D,
//    var cubemap0: samplerCube,

    var colourDiffuse: vec4,
) : UniformMappable {
    override fun map(mapper: ShaderProgram.UniformMapper): Boolean {
        mapper
//            .map("diffuse0", diffuse0)
//            .map("specularTexture", specularTexture)
//            .map("specular0", specular0)
//            .map("shininess", shininess)
//            .map("specularity", specularity)
//            .map("hasNormalMap", hasNormalMap)
//            .map("normal0", normal0)
//            .map("emissiveTexture", emissiveTexture)
//            .map("emissive0", emissive0)
//            .map("cubemap0", cubemap0)
            .map("colourDiffuse", colourDiffuse)
        return true
    }
}
