package org.etieskrill.engine.graphics.texture.animation;

import org.etieskrill.engine.graphics.texture.Texture.MagFilter;
import org.etieskrill.engine.graphics.texture.Texture.MinFilter;
import org.etieskrill.engine.graphics.texture.Texture.Wrapping;
import org.etieskrill.engine.graphics.texture.ArrayTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.util.FileUtilsKt;
import org.joml.Vector2ic;

import java.nio.ByteBuffer;
import java.util.List;

import static org.etieskrill.engine.graphics.texture.animation.TextureAnimationYamlParser.loadAnimationMetadata;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.BufferUtils.zeroBuffer;

public class AnimatedTexture {

    private final ArrayTexture texture;
    private final TextureAnimationMetadata metaData;

    public static AnimatedTextureBuilder builder() {
        return new AnimatedTextureBuilder();
    }

    public static final class AnimatedTextureBuilder {
        private static final String META_FILE_EXTENSION = ".tex-anim.yml";

        private String file;
        private String metaFile;

        private MinFilter minFilter = MinFilter.TRILINEAR;
        private MagFilter magFilter = MagFilter.LINEAR;
        private Wrapping wrapping = Wrapping.REPEAT;

        public void setFile(String file) {
            this.file = file;
        }

        public void setMetaFile(String metaFile) {
            this.metaFile = metaFile;
        }

        public AnimatedTextureBuilder setMipMapping(MinFilter minFilter, MagFilter magFilter) {
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            return this;
        }

        public AnimatedTextureBuilder setWrapping(Wrapping wrapping) {
            this.wrapping = wrapping;
            return this;
        }

        public AnimatedTexture build() {
            if (file == null) throw new NullPointerException("Texture file may not be null");
            if (metaFile == null) {
                metaFile = FileUtilsKt.getFileName(file) + META_FILE_EXTENSION;
            }

            return new AnimatedTexture(this);
        }
    }

    private AnimatedTexture(AnimatedTextureBuilder builder) {
        var textureAtlas = new Texture2D.FileBuilder(builder.file);
        metaData = loadAnimationMetadata(builder.metaFile);

        if (metaData.getFrameSize().x() * metaData.getFrameSize().y() * metaData.getFrames().size()
            != textureAtlas.getTextureData().capacity() / textureAtlas.getFormat().getChannels()) {
            throw new IllegalArgumentException("Metadata size does not match texture size");
        }

        var animatedTexture = new ArrayTexture.BufferBuilder(
                metaData.getFrameSize(),
                metaData.getFrames().size(),
                metaData.getFormat() != null ? metaData.getFormat() : textureAtlas.getFormat());

        animatedTexture.setMipMapping(builder.minFilter, builder.magFilter);
        animatedTexture.setWrapping(builder.wrapping);

        addTextures(textureAtlas, animatedTexture);
        texture = animatedTexture.build();
    }

    private void addTextures(Texture2D.Builder textureAtlas, ArrayTexture.BufferBuilder animatedTexture) {
        Vector2ic textureSize = textureAtlas.getPixelSize();
        int pixelChannels = textureAtlas.getFormat().getChannels();

        ByteBuffer frameTexture = createByteBuffer(metaData.getFrameSize().x() * metaData.getFrameSize().y() * pixelChannels);
        List<TextureAnimationFrame> frames = metaData.getFrames();
        for (int i = 0; i < frames.size(); i++) {
            TextureAnimationFrame frame = frames.get(i);

            frameTexture.rewind();
            zeroBuffer(frameTexture);

            int width = frame.getAtlasArea().lengthX(), height = frame.getAtlasArea().lengthY();
            for (int h = 0; h < height; h++) {
                frameTexture.put(
                        width * h * pixelChannels,
                        textureAtlas.getTextureData(),
                        (textureSize.x() * h + width * i) * pixelChannels,
                        width * pixelChannels
                );
                frameTexture.position(frameTexture.position() + width * pixelChannels);
            }

            animatedTexture.addTexture(frameTexture.rewind());
        }
    }

    public ArrayTexture getTexture() {
        return texture;
    }

    public TextureAnimationMetadata getMetaData() {
        return metaData;
    }

    public Vector2ic getPixelSize() {
        return texture.getPixelSize();
    }

    public Integer getLength() {
        return texture.getLength();
    }

}
