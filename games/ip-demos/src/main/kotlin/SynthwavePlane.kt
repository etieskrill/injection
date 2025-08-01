package io.github.etieskrill.games.ip.demos.synthwave

import io.github.etieskrill.injection.extension.shader.Texture2D
import io.github.etieskrill.injection.extension.shader.dsl.PureShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.rt
import io.github.etieskrill.injection.extension.shader.dsl.std.rgb2vec3
import io.github.etieskrill.injection.extension.shader.float
import io.github.etieskrill.injection.extension.shader.mat4
import io.github.etieskrill.injection.extension.shader.vec2
import io.github.etieskrill.injection.extension.shader.vec3
import io.github.etieskrill.injection.extension.shader.vec4
import org.etieskrill.engine.application.GameApplication
import org.etieskrill.engine.audio.Audio
import org.etieskrill.engine.audio.AudioListener
import org.etieskrill.engine.audio.AudioMode
import org.etieskrill.engine.audio.AudioSource
import org.etieskrill.engine.audio.StereoAudioSource
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.impl.RenderService
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.graphics.camera.PerspectiveCamera
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.model.model
import org.etieskrill.engine.graphics.model.plane
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.input.Input
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.controller.CursorCameraController
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Label
import org.etieskrill.engine.scene.component.Node
import org.etieskrill.engine.scene.component.PlaybackBar
import org.etieskrill.engine.scene.component.VBox
import org.etieskrill.engine.scene.component.WidgetContainer
import org.etieskrill.engine.scene.component.plot.Histogram
import org.etieskrill.engine.scene.component.plot.HistogramScaleMode
import org.etieskrill.engine.util.FixedArrayDeque
import org.etieskrill.engine.window.Cursor
import org.etieskrill.engine.window.Window
import org.etieskrill.engine.window.window
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

fun main() {
    SynthwavePlane().run()
}

@Suppress("MemberVisibilityCanBePrivate")
class SynthwavePlane : GameApplication(window {
    title = "Synthwave Plane"
//    mode = Window.WindowMode.FULLSCREEN
    mode = Window.WindowMode.BORDERLESS
    size = Window.WindowSize.LARGEST_FIT
    vSync = true
}) {

    val transform = Transform()
    val plane = model("plane") { plane(Vector2f(-100f, -100f), Vector2f(100f, 100f)) }
    val shader = GridShader()
    val camera = PerspectiveCamera(window.currentSize)
    val offset = Vector2f()

    val frameBuffer: FrameBuffer
    val frameTexture: Texture2D
    val sunPipeline = PostPassPipeline(SunPostPass(), null, false)

    val audioSource: StereoAudioSource
    val audioListener: AudioListener

    val fpsLabel: Label
    val fpsGraph: Histogram

    val fftGraph: Histogram

    val playbackBar: PlaybackBar

    init {
        window.addCursorInputs(CursorCameraController(camera))
        window.addKeyInputs(Input.of(Input.bind(Keys.CTRL).to { ->
            if (window.cursor.mode == Cursor.CursorMode.DISABLED)
                window.cursor.mode = Cursor.CursorMode.CAPTURED
            else
                window.cursor.mode = Cursor.CursorMode.DISABLED
        }))
        window.cursor.disable()

        camera.setPosition(Vector3f(0f, 1f, 0f))

        val renderService = RenderService(renderer, camera, window.currentSize)
        entitySystem.addService(renderService)
        frameBuffer = renderService.frameBuffer
        frameTexture = renderService.frameBuffer.attachments[BufferAttachmentType.COLOUR0] as Texture2D
        frameBuffer.setClearColour(Vector4f(0.01f, 0f, 0.02f, 1f))

        entitySystem.createEntity()
            .withComponent(transform)
            .withComponent(Drawable(plane, shader.shader as ShaderProgram))

        audioSource = Audio.read(
//            "1khz-sine.ogg"
            "mr.edwardz-little-violet-charlston-boogie-runnrest.ogg"
            //TODO innerbloom might be cool to see dissected too
//            "avlönskt-bad-apple-multilanguage.ogg"
//            "infected-mushroom-spitfire-monstercat-release.ogg"
//            "nightstop-she-dances-in-the-dark.ogg"
//            "pumped-up-kicks-synthwave.ogg"
            , AudioMode.STEREO, true
        ) as StereoAudioSource

        audioListener = Audio.listener

        fpsLabel = Label()
        fpsGraph = Histogram(100, scaleMode = HistogramScaleMode.FIXED, maxValue = 1.1f * window.refreshRate).apply {
            size = Vector2f(400f, 150f)
        }
        fftGraph = Histogram(
            FFT_BINS.toInt(),
            scaleMode = HistogramScaleMode.FIXED,
            maxValue = 0.5f,
            drawSeparators = false
        ).apply { size = Vector2f(600f, 200f) }
        playbackBar = PlaybackBar(audioSource.duration).apply {
            alignment = Node.Alignment.BOTTOM
            size = Vector2f(700f, 40f)
            margin = Vector4f(10f)

            playAction = { audioSource.play() }
            pauseAction = { audioSource.pause() }
            stopAction = { audioSource.stop() }

            action = { audioSource.offsetSeconds = it.inWholeSeconds.toFloat() }
        }
        window.scene = Scene(
            Batch(renderer, window.currentSize),
            VBox(
                WidgetContainer(fpsGraph).apply {
                    text = "GPU"
                    alignment = Node.Alignment.FIXED_POSITION
                    position = Vector2f(50f, 0f)
                    isCollapsed = true
                },
                fpsLabel, //FIXME how in gods name is the fps graph right-side up, and the fft is not??
                fftGraph,
                playbackBar
            ).apply { size = Vector2f(600f, 500f) },
            OrthographicCamera(window.currentSize)
        )
    }

    var frameSample = 0
    var samples = FixedArrayDeque<Int>((window.refreshRate / 8).toInt())
    var averageSample = 0

    val fftAverageMagnitudes = MutableList(FFT_BINS.toInt()) { 0f }

    override fun loop(delta: Double) {
        //TODO add auto-move mode and button - add disappearing "status" message log

        shader.viewPosition = camera.position
        offset.y += delta.toFloat() //TODO bake some standard vars into all shaders
        shader.offset = offset

        audioListener.position = camera.position
        audioListener.direction = camera.direction.normalize()

        val fftMagnitudes = doFFT(audioSource)

        fftMagnitudes.forEachIndexed { t, value ->
            //TODO probably interpolate (spline-esque?) between missing values - the whole histogram rendering
            // shenanigans should just be converted to a proper shader anyways
            //TODO or this (sparse data points) should probably be it's own node... "Line{Chart,Plot,Graph}"?
            val index = floor((-(0.1f / ((t.toFloat() / FFT_BINS) + 0.1f)) + 1f) * FFT_BINS).toInt()
            val sample = (fftAverageMagnitudes[index] + value) / 2
            fftAverageMagnitudes[index] = sqrt(sample / 5000000f)
        }
        fftGraph.values.addAll(fftAverageMagnitudes)

        val sample = audioSource.offsetSamples
        val windowSize = (audioSource.sampleRate / pacer.averageFPS).toInt()
        var sampleValue = 0
        for (i in max(0, sample - windowSize)..sample) {
            sampleValue += abs(audioSource.buffer!![i].toInt())
        }
        sampleValue /= windowSize

        frameSample = sampleValue
        samples.push(((fftMagnitudes[2] + fftMagnitudes[3] + fftMagnitudes[4]) / 3).toInt())
        averageSample = samples.average().toInt()

        playbackBar.time = audioSource.offsetSeconds.toDouble().seconds

        fpsLabel.text = pacer.averageFPS.toInt().toString()
        if (pacer.totalFramesElapsed % 5L == 0L) fpsGraph.values.push(1 / pacer.deltaTimeSeconds.toFloat())
    }

    override fun render() {
        sunPipeline.shader.invCombined = camera.combined.invert(Matrix4f())
        sunPipeline.shader.time = pacer.time.toFloat()
        sunPipeline.shader.intensity = averageSample / 1000000f + 0.5f

        renderer.render(sunPipeline)
    }
}

//TODO move to helper class
const val FFT_BINS = 512L
var fft = FloatFFT_1D(2 * FFT_BINS)
val fftBuffer = MutableList(FFT_BINS.toInt()) { 0f }
const val COS_WINDOW_a0 = 0.5f // <-- hann, hamming: 25/46f
fun windowFunction(t: Float): Float = COS_WINDOW_a0 - (1f - COS_WINDOW_a0) * cos(2 * Math.PI * t).toFloat()

fun doFFT(audioSource: AudioSource): List<Float> {
    val fftWindow = audioSource.sampleRate / 10

    val windowData = FloatArray(fftWindow + 1)
    for (i in 0..<fftWindow) {
        val weaveFactor = if (audioSource is StereoAudioSource) 2 else 1
        val index = (i + audioSource.offsetSamples - fftWindow / 2).coerceIn(0..<audioSource.numSamples) * weaveFactor
        windowData[i] = audioSource.buffer!!.get(index).toFloat() * windowFunction(i / fftWindow.toFloat())
    }

    check(fftWindow > 2 * FFT_BINS)

    fft.realForward(windowData)

    fftBuffer.clear()
    val magnitudes = (0 until FFT_BINS.toInt()).mapTo(fftBuffer) {
        val real = windowData[2 * it]
        val imaginary = windowData[2 * it + 1]
        sqrt(real * real + imaginary * imaginary)
    }

    return magnitudes
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
            val horizon = if (distanceFromCamera > 75) rgb2vec3("8B1D80") * 10
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
    object : ShaderProgram(listOf("SunPostPass.glsl"), false) {}
) {
    class Vertex(override val position: vec4, val fragPosition: vec2) : ShaderVertexData
    class RenderTargets(val colour: RenderTarget)

    private val vertices by const(arrayOf(vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1)))
    private val sunDirection by const(vec3(0, 0.33, 1))

    var invCombined by uniform<mat4>()
    var time by uniform<float>()
    var intensity by uniform<float>()

    //most all of these are yoinked from https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
    private fun rand(n: float): float = fragFunc { fract(sin(n) * 43758.5453123) }
    private fun rand(n: vec2): float = fragFunc { fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453) }
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

    private fun stripSDF(yDir: float, time: float, offset: float): float = fragFunc {
        val interp = (0.1 * time + offset) % 1
        val width = if (interp < 0.5) 0.02
        else 0.025 * (1 - ((interp - 0.5) * 2)) - 0.005
        val y = yDir + interp * 0.6 - 0.4
        abs(y.toFloat() - 0.2f) - width.toFloat()
    }

    //all praise the shader-ssiah https://iquilezles.org/articles/distfunctions/
    private fun sdfSmoothSubtract(base: float, other: float, smoothingFactor: float) = fragFunc {
        val h = clamp(0.5 - 0.5 * (base + other) / smoothingFactor, 0, 1)
        mix(base, -other, h) + smoothingFactor * h * (1 - h)
    }

    override fun program() {
        vertex { Vertex(vec4(vertices[vertexID], 0, 1), vec2(vertices[vertexID])) }
        fragment {
            val ndc = it.fragPosition

            var nearPoint = invCombined * vec4(ndc, -1, 1)
            var farPoint = invCombined * vec4(ndc, 1, 1)

            nearPoint /= nearPoint.w
            farPoint /= farPoint.w

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
            var stripsDistance = stripSDF(direction.y.toFloat(), time, 0f)
            stripsDistance = min(stripsDistance, stripSDF(direction.y.toFloat(), time, 0.2f)).toFloat()
            stripsDistance = min(stripsDistance, stripSDF(direction.y.toFloat(), time, 0.4f)).toFloat()
            stripsDistance = min(stripsDistance, stripSDF(direction.y.toFloat(), time, 0.6f)).toFloat()
            stripsDistance = min(stripsDistance, stripSDF(direction.y.toFloat(), time, 0.8f)).toFloat()

            val sunDistance = acos(dot(normalize(sunDirection), direction)) - 0.224f //~12.8°
            val distance = sdfSmoothSubtract(sunDistance, stripsDistance, 0.005f)
            //... sdfs are pretty sick

            val sunColour = mix(
                vec4(rgb2vec3("8B1D80"), 1),
                vec4(rgb2vec3("FF3700"), 1),
                (direction.y - 0.1).toFloat() * 8.0
            )
            sunColour.xyz *= intensity

            val aa = distance / length(vec2(dFdx(direction.x), dFdy(direction.y)))
            sunColour.a = smoothstep(1, 0, aa)

            if (sunColour.a == 1f) fragColour = sunColour
            else fragColour += sunColour

            RenderTargets(fragColour.rt)
        }
    }
}
