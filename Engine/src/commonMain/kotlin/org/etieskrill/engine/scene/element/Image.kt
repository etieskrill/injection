package org.etieskrill.engine.scene.element

import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureMinFilter
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.graphics.texture.loadTexture2DData
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.util.readResource
import org.joml.Vector4f

class Image(texturePath: String) : Node<Image>() {

    private val texture = Texture2D.createUITexture(texturePath)

    init {
        colour = Vector4f(1f, 1f, 1f, 1f)
    }

    override fun render(batch: Batch) {
        batch.blit(texture, absolutePosition, size, 0f, renderedColour)
    }

}
