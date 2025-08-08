package io.github.etieskrill.games.sails

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.getComponent
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.Renderer
import org.etieskrill.engine.graphics.gl.shader.impl.BlitShader
import org.etieskrill.engine.graphics.pipeline.PostPassPipeline
import org.etieskrill.engine.graphics.texture.Textures
import org.etieskrill.engine.window.Window
import org.joml.Vector2f

class ShipRenderService(private val renderer: Renderer, private val window: Window) : Service {

    private val shipSprite = Textures.ofFile("textures/ship-icon.png")
    private val pipeline = PostPassPipeline<BlitShader>(BlitShader(), null, opaque = false, depthTest = false)

    override fun canProcess(entity: Entity) = entity.hasComponents(NavalTransform::class.java)

    override fun process(
        targetEntity: Entity,
        entities: List<Entity?>,
        delta: Double
    ) {
        val transform = targetEntity.getComponent<NavalTransform>()!!

        pipeline.shader.apply {
            sprite = shipSprite
            useSpriteColour = true

            position = transform.position
            size = Vector2f(100f)
            rotation = transform.rotation

            windowSize = Vector2f(window.currentSize)
        }

        renderer.render(pipeline)
    }

}
