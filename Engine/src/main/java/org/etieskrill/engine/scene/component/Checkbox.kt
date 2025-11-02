package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector3f
import org.joml.Vector4f

class Checkbox(
    var ticked: Boolean = false,
    var enabled: Boolean = true,
    var action: (Boolean) -> Unit
) : Node<Checkbox>() {

    companion object {
        private val sprite = Textures.ofFile("textures/icons/tick-mark-flipped.png")
    }

    init {
        colour = Vector4f(0.25f)
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        if (!enabled || !doesHit(posX, posY)) return false
        if (action == Keys.Action.RELEASE && button == Keys.LEFT_MOUSE.input) {
            ticked = !ticked
            this.action(ticked)
            return true
        }
        return false
    }

    //TODO handle hover

    override fun render(batch: Batch) {
        if (colour.w != 0f) batch.renderBox(Vector3f(absolutePosition, 0f), Vector3f(size, 0f), colour)
        if (ticked) batch.blit(sprite, absolutePosition, size, 0f)
    }

}
