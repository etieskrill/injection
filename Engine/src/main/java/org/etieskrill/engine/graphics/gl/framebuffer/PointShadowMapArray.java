package org.etieskrill.engine.graphics.gl.framebuffer;

import io.github.etieskrill.injection.extension.shader.Texture2DArrayShadow;
import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment.BufferAttachmentType;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MagFilter;
import org.etieskrill.engine.graphics.texture.AbstractTexture.MinFilter;
import org.etieskrill.engine.graphics.texture.CubeMapArrayTexture;
import org.etieskrill.engine.graphics.texture.CubeMapTexture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.joml.Vector4f;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_EDGE;

public class PointShadowMapArray extends ShadowMap<CubeMapArrayTexture> implements Texture2DArrayShadow {

    protected PointShadowMapArray(Vector2ic size, CubeMapArrayTexture texture) {
        super(size, texture);
    }

    public static PointShadowMapArray generate(@NotNull Vector2ic size, int length) {
        return (PointShadowMapArray) new Builder(size, length).build();
    }

    public static class Builder extends ShadowMap.Builder<CubeMapArrayTexture> {
        private final int length;

        public Builder(Vector2ic size, int length) {
            this.size = size;
            this.length = length;
            texture = generateTexture();
            attach(texture, BufferAttachmentType.DEPTH);
        }

        @Override
        protected CubeMapArrayTexture generateTexture() {
            return new CubeMapArrayTexture.BlankBuilder(size, length)
                    .setFormat(DEPTH)
                    .setType(SHADOW)
                    .setMipMapping(MinFilter.LINEAR, MagFilter.LINEAR)
                    .setWrapping(CLAMP_TO_EDGE)
                    .setBorderColour(new Vector4f(1.0f))
                    .build();
        }

        @Override
        protected ShadowMap<CubeMapArrayTexture> createShadowMap() {
            return new PointShadowMapArray(size, texture);
        }
    }

    @Contract("_, _, _ -> new")
    public Matrix4f[] getCombinedMatrices(float near, float far, @NotNull PointLight light) {
        Matrix4f[] matrices = new Matrix4f[CubeMapTexture.NUM_SIDES];
        for (int i = 0; i < matrices.length; i++) matrices[i] = new Matrix4f();
        return getCombinedMatrices(near, far, light, matrices);
    }

    public Matrix4f[] getCombinedMatrices(float near, float far, @NotNull PointLight light, @NotNull Matrix4f[] targets) {
        return ShadowMappingUtils.getCombinedMatrices(getSize(), near, far, light, targets);
    }

}
