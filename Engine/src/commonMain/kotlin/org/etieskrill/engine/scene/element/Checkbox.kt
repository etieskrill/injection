package org.etieskrill.engine.scene.element

import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node
import org.joml.Vector2f
import org.joml.Vector4f

class Checkbox(
    var ticked: Boolean = false,
    var enabled: Boolean = true,
    var action: (Boolean) -> Unit
) : Node<Checkbox>() {

    companion object {
        private val sprite =
            Texture2D.createFromFile("textures/icons/tick-mark-flipped.png", TextureType.DIFFUSE)
    }

    init {
        colour = Vector4f(0.1f)
        size = Vector2f(Font.DEFAULT_FONT_SIZE.toFloat())
        scaleMode = ScaleMode.FIXED
    }

    override fun handleHit(event: KeyEvent, posX: Double, posY: Double): Boolean {
        if (!enabled || !doesHit(posX, posY)) return false
        if (event.action == KeyEventAction.RELEASE
            && event.key == Key.LEFT_MOUSE
        ) {
            ticked = !ticked
            this.action(ticked)
            return true
        }
        return false
    }

    //TODO handle hover

    override fun render(batch: Batch) {
        if (colour.w != 0f) batch.renderBox(absolutePosition, formattedSize, colour)
        if (ticked) batch.blit(sprite, absolutePosition, formattedSize, 0f)
    }

}
