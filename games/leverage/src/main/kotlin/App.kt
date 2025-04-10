package io.github.etieskrill.games.leverage

import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.data.DirectionalLight
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.gl.shader.impl.StaticShader
import org.etieskrill.engine.graphics.gl.shader.impl.globalLights
import org.etieskrill.engine.graphics.gl.shader.impl.lights
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.input.controller.KeyCameraController
import org.etieskrill.engine.util.Loaders
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector3f

fun main() {
    App()
}

class App : GameApplication(window {
    size = Window.WindowSize.LARGEST_FIT
    mode = Window.WindowMode.BORDERLESS
}) {
    lateinit var camera: Camera

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

        camera = PerspectiveCamera(window.currentSize)

        entitySystem.addService(RenderService(renderer, camera, window.currentSize))

        window.addKeyInputs(KeyCameraController(camera))
        window.addCursorInputs(CursorCameraController(camera))

        window.cursor.disable()
    }

    override fun loop(delta: Double) {}
}
