package org.etieskrill.engine.scene.element

import net.bifreus.kml.Vec4
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node

class Image(texturePath: String) : Node<Image>() {

    private val texture = Texture2D.createUITexture(texturePath)

    init {
        colour = Vec4(1f, 1f, 1f, 1f)
    }

    override fun render(batch: Batch) {
        batch.blit(texture, absolutePosition, size, 0f, renderedColour)
    }

}
