package org.etieskrill.engine.entity.service.impl

import net.bifreus.kml.Vec4
import org.lwjgl.opengl.GL11C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D

//TODO move to renderer?
internal actual fun RenderService.drawOutlines() {
    //TODO bind relevant framebuffer
    renderer.context.getTexture(outlineDepthStencilTexture).bind(0)
    glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, windowSize.x, windowSize.y)
    //TODO check on another computer if any command after this one crash the program
    //is the context corrupted or detached? is it a driver-specific issue?

    fullScreenPipeline.shader.colour = Vec4(1f, 0f, 0f, 1f)
    renderer.render(fullScreenPipeline)

    outlinePipeline.shader.outline = outlineTexture
    renderer.render(outlinePipeline)
}
