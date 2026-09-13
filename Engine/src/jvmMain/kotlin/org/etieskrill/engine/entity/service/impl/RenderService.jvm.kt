package org.etieskrill.engine.entity.service.impl

import org.joml.Vector4f
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL

//TODO move to renderer?
internal actual fun RenderService.drawOutlines() {
    //TODO move to platform

    renderer.context.getTexture(outlineDepthStencilTexture).bind(0)
    glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_STENCIL, 0, 0, windowSize.x(), windowSize.y(), 0)

    glEnable(GL_STENCIL_TEST)
    glStencilFunc(GL_EQUAL, 0xFF, 0xFF)
    glStencilMask(0x00)

    fullScreenPipeline.shader.colour = Vector4f(1f, 0f, 0f, 1f)
    renderer.render(fullScreenPipeline)

    glStencilFunc(GL_NOTEQUAL, 0xFF, 0xFF)

    outlinePipeline.shader.outline = outlineTexture
    renderer.render(outlinePipeline)

    glDisable(GL_STENCIL_TEST)
}
