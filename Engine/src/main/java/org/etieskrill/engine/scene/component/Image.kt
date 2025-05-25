package org.etieskrill.engine.scene.component

import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.texture.Textures
import org.joml.Vector4f

class Image(texturePath: String) : Node<Image>() {

    private val texture = Textures.ofFile(texturePath)

    init {
        colour = Vector4f(1f, 1f, 1f, 1f)
    }

    override fun render(batch: Batch) {
        batch.blit(texture, absolutePosition, size, 0f, renderedColour)
    }

}
