package org.etieskrill.engine.graphics.texture.animation;

import lombok.Getter;
import org.etieskrill.engine.graphics.texture.ArrayTexture;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.etieskrill.engine.util.FileUtils;
import org.joml.Vector2ic;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.BufferUtils.zeroBuffer;

@Getter
public class AnimatedTexture {

    private final ArrayTexture texture;
    private final TextureAnimationMetadata metaData;

    public AnimatedTexture(String file) {
        var textureAtlas = new Texture2D.FileBuilder(file);
        FileUtils.TypedFile typedFile = FileUtils.splitTypeFromPath(file);
        metaData = TextureAnimationYamlParser.loadAnimationMetadata(typedFile.getFileName() + ".tex-anim.yml");

        if (metaData.getFrameSize().x() * metaData.getFrameSize().y() * metaData.getFrames().size()
                != textureAtlas.getTextureData().capacity() / textureAtlas.getFormat().getChannels()) {
            throw new IllegalArgumentException("Metadata size does not match texture size");
        }

        var animatedTexture = new ArrayTexture.BufferBuilder(
                metaData.getFrameSize(),
                metaData.getFrames().size(),
                textureAtlas.getFormat());

        addTextures(textureAtlas, animatedTexture);
        texture = animatedTexture.build();
    }

    private void addTextures(Texture2D.FileBuilder textureAtlas, ArrayTexture.BufferBuilder animatedTexture) {
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

}
