package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBuffer;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.gl.framebuffer.RenderBuffer;
import org.etieskrill.engine.graphics.gl.renderer.GLRenderer;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.texture.AbstractTexture.Format;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.graphics.texture.Textures;
import org.joml.Vector2f;
import org.joml.Vector2ic;

import java.util.List;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glDrawArrays;

public class PostProcessingRenderService extends RenderService {

    private static final int GAUSS_BLUR_ITERATIONS = 3;

    private final FrameBuffer frameBuffer;
    private final Texture2D hdrBuffer;
    private final Texture2D bloomBuffer;
    private final ShaderProgram hdrShader;

    private final FrameBuffer blurFrameBuffer1;
    private final Texture2D blurTextureBuffer1;
    private final FrameBuffer blurFrameBuffer2;
    private final Texture2D blurTextureBuffer2;
    private final ShaderProgram gaussBlurShader;

    public PostProcessingRenderService(GLRenderer renderer, Camera camera, Vector2ic windowSize) {
        super(renderer, camera, windowSize);

        this.hdrBuffer = Textures.genBlank(windowSize, Format.RGBA_F16);
        this.bloomBuffer = Textures.genBlank(windowSize, Format.RGBA_F16);
        RenderBuffer depthStencilBuffer = new RenderBuffer(windowSize, RenderBuffer.Type.DEPTH_STENCIL);
        this.frameBuffer = new FrameBuffer.Builder(windowSize)
                .attach(hdrBuffer, BufferAttachmentType.COLOUR0)
                .attach(bloomBuffer, BufferAttachmentType.COLOUR1)
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

                addUniform("hdrBuffer", Uniform.Type.SAMPLER2D);
                addUniform("bloomBuffer", Uniform.Type.SAMPLER2D);
            }
        };

        this.blurTextureBuffer1 = Textures.genBlank(windowSize, Format.RGBA_F16);
        this.blurFrameBuffer1 = new FrameBuffer.Builder(windowSize)
                .attach(blurTextureBuffer1, BufferAttachmentType.COLOUR0)
                .build();
        this.blurTextureBuffer2 = Textures.genBlank(windowSize, Format.RGBA_F16);
        this.blurFrameBuffer2 = new FrameBuffer.Builder(windowSize)
                .attach(blurTextureBuffer2, BufferAttachmentType.COLOUR0)
                .build();

        this.gaussBlurShader = new ShaderProgram() {
            @Override
            protected String[] getShaderFileNames() {
                return new String[]{"GaussBlur.glsl"};
            }

            @Override
            protected void getUniformLocations() {
                addUniform("source", Uniform.Type.SAMPLER2D);
                addUniform("horizontal", Uniform.Type.BOOLEAN);
                addUniform("sampleDistance", Uniform.Type.VEC2, new Vector2f(1));
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
        blurFrameBuffer1.bind();
        renderer.bindNextFreeTexture(gaussBlurShader, "source", bloomBuffer);
        gaussBlurShader.setUniform("horizontal", true);
        gaussBlurShader.setUniform("sampleDistance", new Vector2f(2));
        gaussBlurShader.start();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        boolean blurBuffer1IsTarget = false;
        for (int i = 0; i < GAUSS_BLUR_ITERATIONS * 2 - 1; i++) {
            (blurBuffer1IsTarget ? blurFrameBuffer1 : blurFrameBuffer2).bind();
            renderer.bindNextFreeTexture(gaussBlurShader, "source", blurBuffer1IsTarget ? blurTextureBuffer2 : blurTextureBuffer1);
            gaussBlurShader.setUniform("horizontal", blurBuffer1IsTarget);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            blurBuffer1IsTarget = !blurBuffer1IsTarget;
        }

        FrameBuffer.bindScreenBuffer();

        renderer.bindNextFreeTexture(hdrShader, "hdrBuffer", hdrBuffer);
        renderer.bindNextFreeTexture(hdrShader, "bloomBuffer", blurBuffer1IsTarget ? blurTextureBuffer1 : blurTextureBuffer2);
        hdrShader.start();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); //this render call without any vertex attributes/vertex array is technically undefined behaviour, so add one if it suddenly does not work
    }

    public ShaderProgram getHdrShader() {
        return hdrShader;
    }

}
