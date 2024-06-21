package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.GLRenderer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.framebuffer.RenderBuffer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Format;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector2ic;

import java.util.List;

import static org.lwjgl.opengl.GL11C.*;

public class PostProcessingRenderService extends RenderService {

    private final FrameBuffer frameBuffer;
    private final Texture2D hdrBuffer;
    private final ShaderProgram hdrShader;

    public PostProcessingRenderService(GLRenderer renderer, Camera camera, Vector2ic windowSize) {
        super(renderer, camera, windowSize);

        this.hdrBuffer = Textures.genBlank(windowSize, Format.RGBA_F16);
        RenderBuffer depthStencilBuffer = new RenderBuffer(windowSize, RenderBuffer.Type.DEPTH_STENCIL);
        this.frameBuffer = new FrameBuffer.Builder(windowSize)
                .attach(hdrBuffer, BufferAttachmentType.COLOUR0)
                .attach(depthStencilBuffer, BufferAttachmentType.DEPTH_STENCIL)
                .build();

        this.hdrShader = new ShaderProgram() {
            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"HDR.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("exposure", Uniform.Type.FLOAT, 1f);
                addUniform("reinhard", Uniform.Type.BOOLEAN, true);
            }
        };
    }

    @Override
    public void preProcess(List<Entity> entities) {
        frameBuffer.clear();
        frameBuffer.bind();
        super.preProcess(entities);
    }

    @Override
    public void postProcess(List<Entity> entities) {
        frameBuffer.unbind();

        renderer.bindNextFreeTexture(hdrShader, "hdrBuffer", hdrBuffer);
        hdrShader.start();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); //this render call without any vertex attributes/vertex array is technically undefined behaviour, so add one if it suddenly does not work
    }

    public ShaderProgram getHdrShader() {
        return hdrShader;
    }

}
