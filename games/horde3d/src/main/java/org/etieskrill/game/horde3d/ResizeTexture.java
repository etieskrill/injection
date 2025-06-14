package org.etieskrill.game.horde3d;

import org.etieskrill.engine.util.ResourceReader;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;

import java.nio.ByteBuffer;

import static org.etieskrill.engine.config.ResourcePaths.TEXTURE_PATH;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImageResize.stbir_resize_uint8_srgb;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;

public class ResizeTexture {

    private static final int TARGET_SIZE = 256;

    public static void main(String[] args) {
        for (String fileName : new String[]{
                "Ch10_1001_Diffuse_4k",
                "Ch10_1001_Glossiness_4k",
                "Ch10_1001_Normal_4k",
                "Ch10_1001_Specular_4k"
        }) {
            int[] width = new int[1], height = new int[1], colourChannels = new int[1];

            ByteBuffer textureData = stbi_load_from_memory(
                    ResourceReader.getRawResource(TEXTURE_PATH + fileName + ".png"),
                    width, height, colourChannels, 0
            );

            if (textureData == null) {
                throw new IllegalStateException("Image loading did not complete successfully: " +
                        STBImage.stbi_failure_reason());
            }

            System.out.printf("Loaded %s: %dx%d %d-channel texture\n", fileName, width[0], height[0], colourChannels[0]);

            ByteBuffer resizedTextureData = createByteBuffer(colourChannels[0] * TARGET_SIZE * TARGET_SIZE);

            if (!stbir_resize_uint8_srgb(
                    textureData, width[0], height[0], 0,
                    resizedTextureData, TARGET_SIZE, TARGET_SIZE, 0,
                    colourChannels[0], STBImageResize.STBIR_ALPHA_CHANNEL_NONE, 0
            )) {
                throw new IllegalStateException("Resize operation did not complete successfully: " +
                        STBImage.stbi_failure_reason());
            }

            if (!stbi_write_png(
                    fileName + "_smol.png",
                    TARGET_SIZE, TARGET_SIZE, colourChannels[0],
                    resizedTextureData, 0
            )) {
                throw new IllegalStateException("Image saving did not complete successfully: " +
                        STBImage.stbi_failure_reason());
            }
        }
    }

}
