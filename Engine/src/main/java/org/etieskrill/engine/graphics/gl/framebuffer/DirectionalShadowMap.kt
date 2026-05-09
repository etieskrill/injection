package org.etieskrill.engine.graphics.gl.framebuffer;

import io.github.etieskrill.injection.extension.shader.Texture2DShadow;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Vector2ic;
import org.joml.Vector4f;

import static org.etieskrill.engine.graphics.texture.AbstractTexture.Format.DEPTH;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Type.SHADOW;
import static org.etieskrill.engine.graphics.texture.AbstractTexture.Wrapping.CLAMP_TO_BORDER;

public class DirectionalShadowMap extends ShadowMap<Texture2D> implements Texture2DShadow {

    protected DirectionalShadowMap(Vector2ic size, Texture2D texture) {
        super(size, texture);
    }

    public static DirectionalShadowMap generate(Vector2ic size) {
        return (DirectionalShadowMap) new Builder(size).build();
    }

    public static class Builder extends ShadowMap.Builder<Texture2D> {
        public Builder(Vector2ic size) {
            super(size);
        }

        @Override
        protected Texture2D generateTexture() {
            return new Texture2D.BlankBuilder(size)
                    .setFormat(DEPTH)
                    .setType(SHADOW)
                    .setMipMapping(AbstractTexture.MinFilter.LINEAR, AbstractTexture.MagFilter.LINEAR)
                    .setWrapping(CLAMP_TO_BORDER)
                    .setBorderColour(new Vector4f(1.0f))
                    .build();
        }

        @Override
        protected ShadowMap<Texture2D> createShadowMap() {
            return new DirectionalShadowMap(size, texture);
        }
    }

}
