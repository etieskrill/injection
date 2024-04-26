package org.etieskrill.engine.graphics.gl.framebuffer;

import org.etieskrill.engine.graphics.data.PointLight;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.CubeMapTexture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.joml.Vector4f;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_EDGE;

public class PointShadowMap extends ShadowMap<CubeMapTexture> {

    protected PointShadowMap(Vector2ic size, CubeMapTexture texture) {
        super(size, texture);
    }

    public static PointShadowMap generate(Vector2ic size) {
        return (PointShadowMap) new Builder(size).build();
    }

    public static class Builder extends ShadowMap.Builder<CubeMapTexture> {
        public Builder(Vector2ic size) {
            super(size);
        }

        @Override
        protected CubeMapTexture generateTexture() {
            return new CubeMapTexture.MemoryBuilder(size)
                    .setFormat(DEPTH)
                    .setType(SHADOW)
                    .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                    .setWrapping(CLAMP_TO_EDGE)
                    .setBorderColour(new Vector4f(1.0f))
                    .build();
        }

        @Override
        protected ShadowMap<CubeMapTexture> createShadowMap() {
            return new PointShadowMap(size, texture);
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
