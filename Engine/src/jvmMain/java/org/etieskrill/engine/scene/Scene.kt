package org.etieskrill.engine.scene;

import kotlinx.coroutines.CoroutineScope
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.input.MouseGestureHandler
import org.etieskrill.engine.scene.Node.ScaleMode
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.*

open class Scene(
    var batch: Batch,
    root: Node<*>, //TODO direct root (e.g. label) is not formatted -> add transparent parent container?
    camera: Camera
) : MouseGestureHandler(), KeyInputHandler {

    var root: Node<*> = root
        set(value) {
            field.focusRequestCallback = null
            field.focused = false

            value.focusRequestCallback = focusCallback
            value.focused = true
            focusedNode = value
            field = value
        }

    /**
     * The [Camera] used to render the scene. Should be an [OrthographicCamera] for non-diegetic UI.
     *
     * Setting the camera resets its transform to the standard ui viewport, with the origin in the top-left corner, and
     * the window size as the bottom-right corner.
     */
    var camera: Camera = camera
        set(value) {
            field = value
            field.setRotation(0f, 180f, 0f)
            field.position = Vector3f(camera.viewportSize, 0f) / 2f
        }

    init {
        this.root = root
        this.camera = camera
    }

    var size: Vector2ic = Vector2i(0)
        set(value) {
            (field as Vector2i).set(value)
            root.invalidate()
        }

    private var focusedNode: Node<*>? = root

    private val focusCallback = { node: Node<*> ->
        focusedNode = node
        node.focused = true
        true
    }

    open fun update(delta: Double) {
        root.apply {
            position.set(0f)
            size.set(this@Scene.size)
            scaleMode = ScaleMode.FIXED
            update(delta)

            computeFixedSizes()
            layout()
        }
    }

    open fun render() {
        if (!root.isVisible) return
        batch.combined = camera.combined
        batch.frameBuffer.bind()

        glDisable(GL_DEPTH_TEST) //TODO either this or implement with depth testing
        glDepthMask(false)
        root.render(batch)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
    }

    fun show() = root.show()
    fun hide() = root.hide()

    fun setCoroutineScope(coroutineScope: CoroutineScope) {
        root.rootUiScope = coroutineScope
    }

    override fun invokeClick(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        super.invokeClick(button, action, posX, posY)
        return root.handleHit(button, action, posX, posY)
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        super.invokeMove(posX, posY)
        return root.handleHover(posX, posY)
    }

    override fun invokeDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double) =
        root.handleDrag(deltaX, deltaY, posX, posY)

    override fun invoke(type: Key.Type, key: Int, action: Int, modifiers: Int) =
        root.handleKey(Key(type, key, modifiers), Keys.Action.fromGLFW(action)!!)

    override fun invokeCharacter(character: Char) = (focusedNode ?: root).handleCharacter(character)

}
