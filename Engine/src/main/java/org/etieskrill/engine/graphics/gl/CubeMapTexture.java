package org.etieskrill.engine.graphics.gl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;

//TODO move these and Texture's builders and factory methods into a TextureFactory
public class CubeMapTexture extends Texture {
    
    private static final int SIDES = 6;
    private static final String DIRECTORY = Texture.DIRECTORY + "cubemaps/";
    
    private static final Logger logger = LoggerFactory.getLogger(CubeMapTexture.class);
    
    private final String name;
    
    public static final class CubemapTextureBuilder extends Builder<CubeMapTexture> {
        private final List<TextureData> sides = new ArrayList<>(SIDES);
        
        private String name;
    
        /**
         * Attempts to load all files from a directory with the given name to a {@code CubeMap}.
         */
        public static CubemapTextureBuilder get(String name) {
            File cubemapDir = new File(DIRECTORY + name);
            if (!cubemapDir.isDirectory())
                throw new MissingResourceException("Invalid name", CubeMapTexture.class.getSimpleName(), name);
            return new CubemapTextureBuilder(name,
                    Arrays.stream(cubemapDir.listFiles())
                    .map(File::getName)
                    .toArray(String[]::new));
        }
    
        /**
         * Should you ever find yourself in dire need of having to enumerate a {@code CubeMap}'s texture files manually,
         * here you go.
         */
        public CubemapTextureBuilder(String name, String[] files) {
            super(Type.DIFFUSE);
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
                    //sortedFiles[1] = "../skybox/back.jpg";
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
    
            for (int i = 0; i < sortedFiles.length; i++) {
                for (int j = 0; j < sortedFiles.length; j++) {
                    if (i == j) continue;
                    if (sortedFiles[i].equals(sortedFiles[j])) System.out.println("collision");
                }
            }
    
            for (String file : sortedFiles) {
                TextureData data = Texture.loadFileOrDefault(DIRECTORY + name + "/" + file, Type.DIFFUSE);
                if (format == null) format = data.getFormat();
                if (data.getFormat() != format)
                    throw new IllegalArgumentException("All textures must have the same colour format");
                if (pixelSize.anyEqual(INVALID_PIXEL_SIZE)) pixelSize = data.getPixelSize();
                if (!data.getPixelSize().equals(pixelSize))
                    throw new IllegalArgumentException("All textures must be equally sized");
                sides.add(data);
            }
        }
    
        @Override
        public CubeMapTexture build() {
            logger.debug("Loading {}x{} {}-bit cubemap texture from {}", pixelSize.s(), pixelSize.t(),
                    NR_BITS_PER_COLOUR_CHANNEL * format.getChannels(), name);
    
            CubeMapTexture texture = new CubeMapTexture(name, this);
            int glFormat = format.toGLFormat();
            
            texture.bind(0);
            for (int i = 0; i < sides.size(); i++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, glFormat, pixelSize.s(), pixelSize.t(),
                        0, glFormat, GL_UNSIGNED_BYTE, sides.get(i).getTextureData());
            }
            
            return texture;
        }
    
        @Override
        protected void freeResources() {
            sides.forEach(side -> stbi_image_free(side.getTextureData()));
        }
    }
    
//    TODO !!!
//    TODO okay, so apparently, vectors are fucking wack and should not be used anymore at all, especially in
//    TODO single-threaded environments like this whole shebang
//    TODO !!!
    
    private CubeMapTexture(String name, Builder<CubeMapTexture> builder) {
        super(builder.setTarget(Target.CUBEMAP));
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    //TODO a lil' wonky, but how to more easily solve this?
    @Override
    public void bind(int unit) {
        glBindTexture(GL_TEXTURE_CUBE_MAP, getID());
    }
    
    @Override
    public void unbind(int unit) {
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }
    
}
