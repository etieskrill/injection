package org.etieskrill.engine.graphics.texture;

import io.github.etieskrill.injection.extension.shader.TextureCubeMap;
import org.etieskrill.engine.graphics.gl.framebuffer.FrameBufferAttachment;
import org.etieskrill.engine.util.ResourceReader;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;

//TODO move these and Texture's builders and factory methods into a TextureFactory
public class CubeMapTexture extends AbstractTexture implements FrameBufferAttachment, TextureCubeMap {

    public static final int NUM_SIDES = 6;

    public static final Vector3fc[] FACE_NORMALS = {
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0),
            new Vector3f(0, -1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};

    public static final Vector3fc[] FACE_UPS = {
            new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, 1),
            new Vector3f(0, 0, -1), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0)
    };

    private static final Logger logger = LoggerFactory.getLogger(CubeMapTexture.class);

    private final Vector2ic size;

    public static final class CubemapTextureBuilder extends Builder {
        /**
         * Attempts to load all files from a directory with the given name to a {@code CubeMap}.
         */
        public static CubemapTextureBuilder get(String file) {
            //TODO (for all external/classpath resources) first search in external facility (some folder/s, which is/are
            // specified via config), then fall back to classpath which should contain standard/error resource, then
            // throw exception
            List<String> cubemapFiles = ResourceReader.getClasspathItems(file);
            cubemapFiles = cubemapFiles.stream()
                    .filter(path -> path.endsWith(".png") || path.endsWith(".jpg"))
                    .toList();

            return new CubemapTextureBuilder(cubemapFiles.toArray(String[]::new));
        }

        /**
         * Should you ever find yourself in dire need of having to enumerate a {@code CubeMap}'s texture files manually,
         * here you go.
         */
        public CubemapTextureBuilder(String[] files) {
            if (files == null || files.length != NUM_SIDES || Arrays.stream(files).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Cubemap must have exactly six valid texture files");
            }

            //TODO improve on presorting / decide on whether to actually do it
            String[] sortedFiles = new String[6];
            for (String fileName : files) {
                //TODO modularise - config file with mapping aliases
                if (fileName.contains("right") || fileName.contains("px")) {
                    sortedFiles[0] = fileName;
                    continue;
                }
                if (fileName.contains("left") || fileName.contains("nx")) {
                    sortedFiles[1] = fileName;
                    continue;
                }
                if (fileName.contains("top") || fileName.contains("up") || fileName.contains("py")) {
                    sortedFiles[2] = fileName;
                    continue;
                }
                if (fileName.contains("bottom") || fileName.contains("down") || fileName.contains("ny")) {
                    sortedFiles[3] = fileName;
                    continue;
                }
                if (fileName.contains("front") || fileName.contains("pz")) {
                    sortedFiles[4] = fileName;
                    continue;
                }
                if (fileName.contains("back") || fileName.contains("nz")) {
                    sortedFiles[5] = fileName;
                    continue;
                }

                logger.info("Could not identify file name for cubemap: {}", fileName);
                sortedFiles = files;
                break;
            }

            for (String fileName : sortedFiles) {
                TextureData data = Textures.loadFileOrDefault(fileName, Type.DIFFUSE);
                if (format == null) format = data.format();
                if (data.format() != format)
                    throw new IllegalArgumentException("All textures must have the same colour format");
                if (pixelSize.equals(INVALID_PIXEL_SIZE, INVALID_PIXEL_SIZE)) pixelSize = data.pixelSize();
                if (!data.pixelSize().equals(pixelSize))
                    throw new IllegalArgumentException("All textures must be equally sized");
                sides.add(data);
            }
        }

        @Override
        protected void freeResources() {
            sides.forEach(side -> stbi_image_free(side.textureData()));
        }
    }

    public static class MemoryBuilder extends Builder {
        public MemoryBuilder(Vector2ic pixelSize) {
            for (int i = 0; i < NUM_SIDES; i++)
                sides.add(new TextureData(null, pixelSize, null));
            this.pixelSize = pixelSize;
        }

        @Override
        protected void freeResources() {
        }
    }

    private static abstract class Builder extends AbstractTexture.Builder<CubeMapTexture> {
        protected final List<TextureData> sides = new ArrayList<>(NUM_SIDES);

        @Override
        protected CubeMapTexture bufferTextureData() {
            logger.debug("Loading {}x{} {}-bit {} {} cubemap texture from {}", pixelSize.x(), pixelSize.y(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), format.name().toLowerCase(),
                    type.name().toLowerCase(), getClass().getSimpleName());

            CubeMapTexture texture = new CubeMapTexture(this);

            texture.bind(0);
            for (int i = 0; i < sides.size(); i++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, format.toGlInternalFormat(),
                        pixelSize.x(), pixelSize.y(), 0, format.toGLFormat(), GL_UNSIGNED_BYTE,
                        sides.get(i).textureData());
            }

            return texture;
        }
    }

    private CubeMapTexture(Builder builder) {
        super(builder.setTarget(Target.CUBEMAP));
        this.size = builder.pixelSize;
    }

    @Override
    public Vector2ic getSize() {
        return size;
    }

    @Override
    public void attach(BufferAttachmentType type) {
        //This call binds the whole cubemap as a single shader object, where the faces are then
        //addressed using gl_Layer. The built-in variable does NOT work if we bound every face of the
        //cubemap using glFramebufferTexture2D, as the texture object's id would then refer to only the
        //last texture specified this way, which, when iterating over the faces, is the negative z one.
        glFramebufferTexture(GL_FRAMEBUFFER, type.toGLAttachment(), getID(), 0);
    }
}
