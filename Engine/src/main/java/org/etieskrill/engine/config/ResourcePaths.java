package org.etieskrill.engine.config;

/**
 * A utility class holding the path configuration for all resource types packaged directly into the engine's JAR,
 * and thus available on any classpath which depends on this module.
 */
public class ResourcePaths {

    public static final String FONT_PATH = "fonts/";

    public static final String MODEL_PATH = "models/";

    public static final String SHADER_PATH = "shaders/";

    public static final String TEXTURE_PATH = "textures/";
    public static final String TEXTURE_CUBEMAP_PATH = "cubemaps/";
    public static final String CUBEMAP_PATH = TEXTURE_PATH + TEXTURE_CUBEMAP_PATH;

    private ResourcePaths() {}

}
