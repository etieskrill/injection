package io.github.etieskrill.games.ip.demos.synthwave

import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.times

fun main() {
    SynthwavePlane().run()
}

class SynthwavePlane : GameApplication(window {
    title = "Synthwave Plane"
    mode = Window.WindowMode.FULLSCREEN
    size = Window.WindowSize.LARGEST_FIT
    vSync = true
}) {

    val transform = Transform()
    val plane = model("plane") { plane(Vector2f(-100f, -100f), Vector2f(100f, 100f)) }
    val shader = GridShader()
    val camera = PerspectiveCamera(window.currentSize)

    override fun init() {
        window.addCursorInputs(CursorCameraController(camera))
        window.cursor.disable()

        camera.setPosition(Vector3f(0f, 1f, 0f))

        entitySystem.addService(RenderService(renderer, camera, window.currentSize))
        entitySystem.createEntity()
            .withComponent(transform)
            .withComponent(Drawable(plane, shader.shader as ShaderProgram))

        renderer.setClearColour(Vector3f(0.01f, 0f, 0.02f))
    }

    override fun loop(delta: Double) {
        shader.viewPosition = camera.position
    }
}

class GridShader : ShaderBuilder<GridShader.InputVertex, GridShader.Vertex, GridShader.RenderTargets>(
    object : ShaderProgram(listOf("Grid.glsl")) {} //FIXME this is stoopid too
) {
    data class InputVertex(val position: vec3) //FIXME org.etieskrill.engine.graphics.model.Vertex does not work?
    data class Vertex(override val position: vec4, val fragPosition: vec4) : ShaderVertexData
    data class RenderTargets(
        val fragColour: RenderTarget,
        val bloomColour: RenderTarget
    ) //FIXME prohibit name clashes with data structs where structs are unwound (e.g. here bloomColour "bloom" in fragment shader)

    var viewPosition by uniform<vec3>()
    var model by uniform<mat4>() //TODO base shader with "pipeline" uniforms
    var combined by uniform<mat4>()

    override fun program() {
        vertex {
            val fragPosition = vec4(it.position, 1)
            Vertex(
                combined * model * fragPosition, //FIXME usage in fragment shader should auto generate a struct + use it
                fragPosition
            )
        }
        fragment {
            val distanceFromCamera = length(vec3(it.fragPosition) - viewPosition)

            val width = 0.04
            val red = if (it.fragPosition.x % 1 < width || it.fragPosition.z % 1 < width) 0.35 else 0.0
            val blue = if (it.fragPosition.x % 1 > 1 - width || it.fragPosition.z % 1 > 1 - width) 1 else 0

            var grid = vec3(0.005, 0, 0.005) //base colour

            grid.x += red //red grid lines
            grid.z += blue //blue grid lines

            val horizon = if (distanceFromCamera > 75) vec3(
                0.545,
                0.114,
                0.502
            ) * 10//vec3(8B1D80) TODO add 255 and hex colour constructors/conversions
            else vec3(0) //blooming circular horizon

            grid += horizon

            val brightness = dot(grid, vec3(0.2126, 0.7152, 0.0722))
            val bloom = if (brightness > 1) vec4(grid, 1)
            else vec4(0, 0, 0, 1)

            RenderTargets(vec4(grid, 1).rt, bloom.rt)
        }
    }
}
