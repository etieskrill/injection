package io.github.etieskrill.games.ip.demos.synthwave

import io.github.etieskrill.injection.extension.shader.Texture2D
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.times
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL45C.glCreateVertexArrays

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
    val shader = GridShader()// object : ShaderProgram(listOf("Dissect.glsl")) {}
    val camera = PerspectiveCamera(window.currentSize)
    val dummyVAO = glCreateVertexArrays()
    val offset = Vector2f()

    val frameBuffer: FrameBuffer
    val frameTexture: Texture2D
    val sunShader = SunPostPass()

    init {
        window.addCursorInputs(CursorCameraController(camera))
        window.cursor.disable()

        camera.setPosition(Vector3f(0f, 1f, 0f))

        val renderService = RenderService(renderer, camera, window.currentSize)
        entitySystem.addService(renderService)
        frameBuffer = renderService.frameBuffer
        frameTexture = renderService.frameBuffer.attachments[BufferAttachmentType.COLOUR0] as Texture2D

        entitySystem.createEntity()
            .withComponent(transform)
            .withComponent(Drawable(plane, shader.shader as ShaderProgram))

        renderer.setClearColour(Vector3f(0.01f, 0f, 0.02f))
//        renderer.setClearColour(Vector3f(0f))

//        window.addKeyInputs { type, key, action, modifiers ->
//              //TODO ah wannah moove
//        }
    }

    override fun loop(delta: Double) {
//        glBindVertexArray(dummyVAO)
//        shader.start()
//        shader.setUniform("iResolution", window.currentSize, false)
//        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        //TODO add auto-move mode and button - add disappearing "status" message log

        shader.viewPosition = camera.position
        offset.y += delta.toFloat()
        shader.offset = offset
    }

    override fun render() {
        sunShader.start()
        sunShader.invCombined = camera.combined.invert(Matrix4f())
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glBlendFunc(GL_ONE, GL_ZERO);
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
    var offset by uniform<vec2>()
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

            val width = 0.01
            val aa = fwidth(it.fragPosition.xz) * 2
            var componentLines = abs((it.fragPosition.xz + offset) % 1 * 2 - 1)
            componentLines = smoothstep(width + aa, width - aa, componentLines)
            var lines = vec2(componentLines.x + componentLines.y) //grid lines
            lines *= min(1, exp(-.25 * (distanceFromCamera - 5)))

            var grid = vec3(0.005, 0, 0.005) //base colour

            grid.xz += lines

            //FIXME grid += if (...) ... is not extrapolated
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

            bloom.xz += lines //TODO while this is technically not the way bloom is supposed to work, it does give a lot more control

            RenderTargets(vec4(grid, 1).rt, bloom.rt)
        }
    }
}

class SunPostPass : PureShaderBuilder<SunPostPass.Vertex, SunPostPass.RenderTargets>(
    object : ShaderProgram(listOf("SunPostPass.glsl")) {}
) {
    class Vertex(override val position: vec4, val fragPosition: vec2) : ShaderVertexData
    class RenderTargets(val colour: RenderTarget)

    val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))
    val sunDirection by const(vec3(0, 0.33, 1))

    var invCombined by uniform<mat4>()

    //most all of these are yoinked from https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
    fun rand(n: float): float = fragFunc { fract(sin(n) * 43758.5453123) }
    fun rand(n: vec2): float = fragFunc { fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453) }
//    fun noise(n: vec2): float = fragFunc {
//        val d = vec2(0.0, 1.0)
//        val b = floor(n)
//        val f = smoothstep(vec2(0.0), vec2(1.0), fract(n))
//
//        val noise: float
//        noise = mix(mix(rand(b), rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y)
//        noise = mix(mix(bRand, rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y)
//        noise
//    }

//    fun mod289(x: float): float = fragFunc { x - floor(x * (1.0 / 289.0)) * 289.0f }
//    fun mod289(x: vec4): vec4 = fragFunc { x - floor(x * (1.0 / 289.0)) * 289.0 }
//    fun perm(x: vec4): vec4 = fragFunc { mod289(((x * 34.0) + 1.0) * x) }
//
//    fun noise(p: vec3): float = fragFunc {
//        val a = floor(p);
//        var d = p - a;
//        d = d * d * (3.0 - 2.0 * d);
//
//        val b = a.xxyy + vec4(0.0, 1.0, 0.0, 1.0);
//        val k1 = perm(b.xyxy);
//        val k2 = perm(k1.xyxy + b.zzww);
//
//        vec4 c = k2 + a.zzzz;
//        vec4 k3 = perm(c);
//        vec4 k4 = perm(c + 1.0);
//
//        vec4 o1 = fract(k3 * (1.0 / 41.0));
//        vec4 o2 = fract(k4 * (1.0 / 41.0));
//
//        vec4 o3 = o2 * d.z + o1 * (1.0 - d.z);
//        vec2 o4 = o3.yw * d.x + o3.xz * (1.0 - d.x);
//
//        return o4.y * d.y + o4.x * (1.0 - d.y);
//    }

    override fun program() {
        vertex { Vertex(vec4(vertices[vertexID], 0, 1), vec2(vertices[vertexID])) }
        fragment {
            val ndc = it.fragPosition

            var nearPoint = invCombined * vec4(ndc, -1, 1)
            var farPoint = invCombined * vec4(ndc, 1, 1)

            nearPoint = nearPoint / nearPoint.w
            farPoint = farPoint / farPoint.w

            val direction = normalize(farPoint.xyz - nearPoint.xyz)

            var fragColour = vec4(0)

            //an extremely crude random star generator
            val offset = vec2(rand(ndc.x.toFloat()), rand(ndc.y.toFloat()))
            val size = rand(direction.xy * 100) * 0.1

            val frac = abs(fract((direction/* + vec3(offset * 0.001, 0)*/) * 50) * 2 - 1)
            if (direction.y > 0 && length(frac) < size) {
                fragColour = vec4(0.5, 1, 1, 1)
            }

            //"sun" - interpolation variable still needs to be perspective adjusted
            val strips = direction.y > 0.21 && direction.y < 0.23
                    || direction.y > 0.235 && direction.y < 0.245
                    || direction.y > 0.25 && direction.y < 0.26
            if (dot(normalize(sunDirection), direction) > 0.975 && !strips) {
                val t = (direction.y - 0.1).toFloat() * 8.0
                fragColour = mix(vec4(0.545, 0.114, 0.502, 1), vec4(1, 0.216, 0, 1) /*FF7238*/, t)
            }

            RenderTargets(fragColour.rt)
        }
    }
}
