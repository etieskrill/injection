package org.etieskrill.engine.entity.service.impl

import org.joml.Vector4f
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D

//TODO move to renderer?
internal actual fun RenderService.drawOutlines() {
    //TODO bind relevant framebuffer
    renderer.context.getTexture(outlineDepthStencilTexture).bind(0)
    glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, windowSize.x(), windowSize.y())

    fullScreenPipeline.shader.colour = Vector4f(1f, 0f, 0f, 1f)
    renderer.render(fullScreenPipeline)

    outlinePipeline.shader.outline = outlineTexture
    renderer.render(outlinePipeline)
}
