package org.etieskrill.engine.graphics.texture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.etieskrill.engine.config.ResourcePaths.CUBEMAP_PATH;
import static org.etieskrill.engine.config.ResourcePaths.TEXTURE_CUBEMAP_PATH;
import static org.etieskrill.engine.graphics.texture.Textures.NR_BITS_PER_COLOUR_CHANNEL;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;

//TODO move these and Texture's builders and factory methods into a TextureFactory
public class CubeMapTexture extends AbstractTexture {
    
    private static final int SIDES = 6;

    private static final Logger logger = LoggerFactory.getLogger(CubeMapTexture.class);
    
    private final String name;
    
    public static final class CubemapTextureBuilder extends Builder<CubeMapTexture> {
        private final List<TextureData> sides = new ArrayList<>(SIDES);
        
        private final String name;
    
        /**
         * Attempts to load all files from a directory with the given name to a {@code CubeMap}.
         */
        public static CubemapTextureBuilder get(String name) {
            //TODO (for all external/classpath resources) first search in external facility (some folder/s, which is/are
            // specified via config), then fall back to classpath which should contain standard/error resource, then
            // throw exception
            List<String> cubemapFiles;
            URL cubemapUrl = CubeMapTexture.class.getClassLoader().getResource(CUBEMAP_PATH + name);
            if (cubemapUrl == null)
                throw new MissingResourceException("Cubemap could not be found",
                    CubeMapTexture.class.getSimpleName(), name);
            try (FileSystem fs = FileSystems.newFileSystem(cubemapUrl.toURI(), Map.of())) {
                Path cubemapPath = fs.getPath(CUBEMAP_PATH + name);
                PathMatcher matcher = fs.getPathMatcher("glob:**/*.{png,jpg}");
                try (Stream<Path> files = Files.find(cubemapPath, 5, (path, attributes) ->
                        !attributes.isDirectory() && attributes.isRegularFile() && matcher.matches(path)
                )) {
                    cubemapFiles = files
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toList();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list files from classpath", e);
            } catch (URISyntaxException e) {
                throw new RuntimeException("Internal exception", e);
            }
            return new CubemapTextureBuilder(name, cubemapFiles.toArray(String[]::new));
        }
    
        /**
         * Should you ever find yourself in dire need of having to enumerate a {@code CubeMap}'s texture files manually,
         * here you go.
         */
        public CubemapTextureBuilder(String name, String[] files) {
            this.name = name;
            
            if (files == null || files.length != SIDES || Arrays.stream(files).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Cubemap must have exactly six valid texture files");
            }
    
            //TODO improve on presorting / decide on whether to actually do it
            String[] sortedFiles = new String[6];
            for (String file : files) {
                if (file.contains("right") || file.contains("px")) {
                    sortedFiles[0] = file;
                    continue;
                }
                if (file.contains("left") || file.contains("nx")) {
                    sortedFiles[1] = file;
                    continue;
                }
                if (file.contains("top") || file.contains("up") || file.contains("py")) {
                    sortedFiles[2] = file;
                    continue;
                }
                if (file.contains("bottom") || file.contains("down") || file.contains("ny")) {
                    sortedFiles[3] = file;
                    continue;
                }
                if (file.contains("front") || file.contains("pz")) {
                    sortedFiles[4] = file;
                    continue;
                }
                if (file.contains("back") || file.contains("nz")) {
                    sortedFiles[5] = file;
                    continue;
                }
                
                logger.info("Could not identify file name for cubemap: " + file);
                sortedFiles = files;
                break;
            }
    
            for (String file : sortedFiles) {
                TextureData data = Textures.loadFileOrDefault(TEXTURE_CUBEMAP_PATH + name + "/" + file, Type.DIFFUSE);
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
        protected CubeMapTexture bufferTextureData() {
            logger.debug("Loading {}x{} {}-bit cubemap texture from {}", pixelSize.x(), pixelSize.y(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), name);
    
            CubeMapTexture texture = new CubeMapTexture(name, this);

            texture.bind(0);
            for (int i = 0; i < sides.size(); i++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, format.toGlInternalFormat(),
                        pixelSize.x(), pixelSize.y(), 0, format.toGLFormat(), GL_UNSIGNED_BYTE,
                        sides.get(i).textureData());
            }
            
            return texture;
        }
    
        @Override
        protected void freeResources() {
            sides.forEach(side -> stbi_image_free(side.textureData()));
        }
    }
    
    private CubeMapTexture(String name, Builder<CubeMapTexture> builder) {
        super(builder.setType(Type.DIFFUSE).setTarget(Target.CUBEMAP));
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

}
