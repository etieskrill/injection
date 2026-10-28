package org.etieskrill.engine.scene

import kotlinx.coroutines.CoroutineScope
import net.bifreus.kml.Vec2i
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec3
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyInputHandler
import org.etieskrill.engine.input.MouseGestureHandler
import org.etieskrill.engine.scene.Node.ScaleMode

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
            field.position = Vec3(camera.viewportSize, 0f) / 2f
        }

    init {
        this.root = root
        this.camera = camera
    }

    var size: Vec2ic = Vec2i(0)
        set(value) {
            (field as Vec2i)(value)
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
            position(0)
            size(this@Scene.size)
            scaleMode = ScaleMode.FIXED
            update(delta)

            computeFixedSizes()
            layout()
        }
    }

    open fun render() {
        if (!root.isVisible) return

        batch.combined = camera.combined
        root.render(batch)
    }

    fun show() = root.show()
    fun hide() = root.hide()

    fun setCoroutineScope(coroutineScope: CoroutineScope) {
        root.rootUiScope = coroutineScope
    }

    override fun invokeClick(button: KeyEvent, posX: Double, posY: Double): Boolean {
        super.invokeClick(button, posX, posY)
        return root.handleHit(button, posX, posY)
    }

    override fun invokeMove(posX: Double, posY: Double): Boolean {
        super.invokeMove(posX, posY)
        return root.handleHover(posX, posY)
    }

    override fun invokeDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double) =
        root.handleDrag(deltaX, deltaY, posX, posY)

    override fun invoke(event: KeyEvent): Boolean = root.handleKey(event)

    override fun invokeCharacter(character: Char) = (focusedNode ?: root).handleCharacter(character)

}
