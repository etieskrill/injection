package org.etieskrill.engine.graphics.gl.shader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.config.GLContextConfig;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.util.FileUtils;
import org.etieskrill.engine.util.ResourceReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.etieskrill.engine.config.ResourcePaths.SHADER_PATH;
import static org.etieskrill.engine.graphics.gl.GLUtils.*;
import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.ShaderType.*;
import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.INVALID_UNIFORM_LOCATION;
import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.NESTED_UNIFORM_LOCATION;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL46C.*;

public abstract class ShaderProgram implements Disposable {

    public static boolean AUTO_START_ON_VARIABLE_SET = true;
    public static boolean CLEAR_ERROR_BEFORE_SHADER_CREATION = true;

    //TODO move placeholder shader here
    private static final String DEFAULT_VERTEX_FILE = SHADER_PATH + "Phong.vert";
    private static final String DEFAULT_FRAGMENT_FILE = SHADER_PATH + "Phong.frag";

    private boolean STRICT_UNIFORM_DETECTION = true;

    protected int programID;
    private int vertID, geomID = -1, fragID;
    private boolean geometry = false;
    private final Map<String, Uniform> uniforms;
    private final Map<String, ArrayUniform> arrayUniforms;

    @Getter
    private final boolean placeholder;

    private final Map<String, Integer> nonstrictUniformCache = new HashMap<>();
    private final Set<String> unregisteredUniforms = new HashSet<>();
    private final Set<String> missingUniforms = new HashSet<>();

    private final int MAX_TEXTURE_UNITS;
    private int currentTextureUnit;
    private final Map<String, Integer> boundTextures; //TODO this is actually per context, so it could use a ThreadLocal - it also causes incoherent state if #start() is not called properly

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public enum ShaderType {
        VERTEX,
        GEOMETRY,
        FRAGMENT,
        COMPOSITE,
        LIBRARY //TODO probably remove??
    }

    @Getter
    @AllArgsConstructor
    protected static final class ShaderFile {
        private final String name;
        private final ShaderType type;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShaderFile that = (ShaderFile) o;
            return Objects.equals(name, that.name) && type == that.type;
        }

        @Override
        public String toString() {
            return "[%s, %s]".formatted(name, type.name().toLowerCase());
        }
    }

    /**
     * A shader file with the <i>glsl</i> extension is presumed to contain exactly a vertex shader, a fragment shader
     * and - if the flag was set with {@link #hasGeometryShader()} - a geometry shader in the corresponding
     * definition guards.
     */
    protected ShaderProgram() {
        Set<ShaderFile> files = Arrays.stream(getShaderFileNames()).map(file -> {
            var typedFile = FileUtils.splitTypeFromPath(file);
            ShaderType type = switch (typedFile.getExtension()) {
                case "vert" -> VERTEX;
                case "geom" -> GEOMETRY;
                case "frag" -> FRAGMENT;
                case "glsl" -> COMPOSITE;
                default -> throw new ShaderCreationException("Cannot load shader with unknown file extension: " + file);
            };
            return new ShaderFile(file, type);
        }).collect(Collectors.toSet());

        if (files.size() == 1 && !files.stream().allMatch(file -> file.getType() == COMPOSITE)) {
            throw new ShaderCreationException("Single-file shaders must have 'glsl' extension");
        } else if (files.size() != 1 && !files.stream()
                .map(ShaderFile::getType)
                .collect(Collectors.toSet())
                .containsAll(Set.of(VERTEX, FRAGMENT))) {
            throw new ShaderCreationException("Shader must have one vertex and one fragment shader");
        }
        //TODO create spec for other shader types and shader programs split across multiple files
        // consider just creating a standard, such that only a single unique identifier must be passed

        logger.debug("Creating shader from files: {}", files);

        this.uniforms = new HashMap<>();
        this.arrayUniforms = new HashMap<>();

        this.MAX_TEXTURE_UNITS = GLContextConfig.getMaxTextureUnits();
        this.currentTextureUnit = 0;
        this.boundTextures = new HashMap<>(MAX_TEXTURE_UNITS);

        boolean placeholder = false;
        try {
            createShader(files);
        } catch (ShaderException e) {
            logger.warn("Exception during shader creation, using default shader", e); //TODO remove and add explicit alternative declaration
            boolean prevStrictState = STRICT_UNIFORM_DETECTION;
            STRICT_UNIFORM_DETECTION = false; //TODO quick and dirty solution bcs faulty shader's uniforms are still added
            createShader(Set.of(
                    new ShaderFile(DEFAULT_VERTEX_FILE, VERTEX),
                    new ShaderFile(DEFAULT_FRAGMENT_FILE, FRAGMENT)
            ));
            STRICT_UNIFORM_DETECTION = prevStrictState;
            placeholder = true;
        }

        this.placeholder = placeholder;

        if (checkError("OpenGL error during shader creation"))
            logger.info("Successfully created shader");
    }

    private void createShader(Set<ShaderFile> files) {
        init();
        if (CLEAR_ERROR_BEFORE_SHADER_CREATION) clearError();
        if (files.size() > 1) createProgram(files);
        else createSingleFileProgram(files.stream().findAny().get());

        start();
        getUniformLocations();
        stop();
    }

    private void createProgram(Set<ShaderFile> files) {
        programID = glCreateProgram();

        vertID = loadShader(files, VERTEX);
        glAttachShader(programID, vertID);

        if (files.stream().map(ShaderFile::getType).anyMatch(type -> type == GEOMETRY)) {
            geomID = loadShader(files, GEOMETRY);
            glAttachShader(programID, geomID);
        }

        fragID = loadShader(files, FRAGMENT);
        glAttachShader(programID, fragID);

        checkLinkStatus();
    }

    private void createSingleFileProgram(ShaderFile file) {
        programID = glCreateProgram();

        vertID = loadShader(file, VERTEX);
        glAttachShader(programID, vertID);
        if (geometry) {
            try {
                geomID = loadShader(file, GEOMETRY);
                glAttachShader(programID, geomID);
            } catch (ShaderCreationException e) {
                throw new ShaderCreationException("Failed to compile geometry shader from composite shader file", e);
            }
        }
        fragID = loadShader(file, FRAGMENT);
        glAttachShader(programID, fragID);

        checkLinkStatus();
    }

    private void checkLinkStatus() {
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) != GL_TRUE)
            throw new ShaderCreationException("Shader program could not be linked", glGetProgramInfoLog(programID));

        disposeShaders();

        //TODO debug manual / why links may fail
        // - cannot link without reason
        //   - geometry shader in/output primitives not defined
        // - no display with geometry shader
        //   - geometry shader does not call EmitVertex/EndPrimitive

        //TODO write test engine to validate shaders and such pre-launch/via a separate script (unit-test-esque)
        //this actually validates based on the current OpenGL state, meaning that here, a completely uninitialised
        //program is being tested, which will, in the majority of cases, fail.
//        glValidateProgram(programID);
//        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE)
//            throw new IllegalStateException("Shader program was not successfully validated\n%s"
//                    .formatted(glGetProgramInfoLog(programID)));

        checkError("Error while creating shader");
    }

    private int loadShader(Set<ShaderFile> files, ShaderType type) {
        return loadShader(files.stream()
                        .filter(shaderFile -> shaderFile.getType() == type)
                        .findAny()
                        .orElseThrow(() -> new ShaderCreationException("No " + type.name().toLowerCase() + " shader file was found")),
                type);
    }

    //TODO add loader for shader objects and wrap calls to this method in said loader
    private int loadShader(ShaderFile file, ShaderType type) {
        logger.trace("Loading {} shader from file: {}", file.getType().name().toLowerCase(), file);

        int shaderID = glCreateShader(switch (type) {
            case VERTEX -> GL_VERTEX_SHADER;
            case GEOMETRY -> GL_GEOMETRY_SHADER;
            case FRAGMENT -> GL_FRAGMENT_SHADER;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        });

        String shaderSource = getShaderSource(file);
        if (file.getType() == COMPOSITE) {
            shaderSource = injectShaderCompileDirective(shaderSource, type);
        }
        glShaderSource(shaderID, shaderSource);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) != GL_TRUE)
            throw new ShaderCreationException("Failed to compile %s shader from %s"
                    .formatted(file.getType().name().toLowerCase(), file), glGetShaderInfoLog(shaderID));

        return shaderID;
    }

    private String getShaderSource(ShaderFile file) {
        String qualifiedName = SHADER_PATH + file.getName();
        return ResourceReader.getClasspathResource(qualifiedName);
    }

    private String injectShaderCompileDirective(String shaderSource, ShaderType type) {
        List<String> shaderSourceLines = new ArrayList<>(shaderSource.lines().toList());
        for (int i = 0; i < shaderSourceLines.size(); i++) {
            String line = shaderSourceLines.get(i);
            if (line.isBlank()) continue;
            shaderSourceLines.add(i + 1, "#define " + type.name() + "_SHADER");
            break;
        }
        return String.join("\n", shaderSourceLines);
    }

    //TODO use named string arbs to modularise shaders
    private void loadLibrary(String file) {
        glNamedStringARB(GL_SHADER_INCLUDE_ARB, file.split("\\.")[0], ResourceReader.getClasspathResource(file));
    }

    private void disposeShaders() {
        glDeleteShader(vertID);
        glDeleteShader(fragID);
    }

    public void start() {
        currentTextureUnit = 0;
        boundTextures.clear();
        bind();
    }

    private void bind() {
        glUseProgram(programID);
    }

    public void stop() {
        glUseProgram(0);
    }

    public void setUniform(@NotNull String name, @NotNull Object value) {
        setUniform(name, value, uniforms, true, false);
    }

    public void setUniformNonStrict(@NotNull String name, @NotNull Object value) {
        setUniform(name, value, uniforms, false, false);
    }

    public void setUniform(@NotNull String name, @NotNull Object value, boolean strict) {
        setUniform(name, value, uniforms, strict, false);
    }

    public void setUniformArray(@NotNull String name, @NotNull Object[] values) {
        if (values.length == 0) return;
        setUniform(name, values, arrayUniforms, true, true);
    }

    public void setUniformArrayNonStrict(@NotNull String name, @NotNull Object[] values) {
        if (values.length == 0) return;
        setUniform(name, values, arrayUniforms, false, true);
    }

    public void setUniformArray(@NotNull String name, int index, @NotNull Object value) {
        setUniform(name + "[" + index + "]", value, false);
    }

    /**
     * Binds a {@link AbstractTexture Texture} to a shader's uniform sampler. This requires {@link #start()} to be
     * called before beginning a render pass and before calling this method to work properly.
     *
     * @param name    the sampler name in the shader
     * @param texture the texture to be bound
     */
    public void setTexture(@NotNull String name, @NotNull AbstractTexture texture) {
        setTexture(name, texture, true);
    }

    /**
     * Binds a {@link AbstractTexture Texture} to a shader's uniform sampler called {@code name}. This requires {@link #start()} to be
     * called before beginning a render pass and before calling this method to work properly.
     *
     * @param name    the sampler name in the shader
     * @param texture the texture to be bound
     */
    public void setTexture(@NotNull String name, @NotNull AbstractTexture texture, boolean strict) {
        int unit;
        var boundUnit = boundTextures.get(name);
        if (boundUnit != null) {
            unit = boundUnit;
        } else {
            if (currentTextureUnit + 1 > MAX_TEXTURE_UNITS) {
                throw new ShaderUniformException("No texture unit available");
            }

            unit = currentTextureUnit++;
            boundTextures.put(name, unit);
        }

        //TODO validate texture type

        setUniform(name, unit, strict);
        texture.bind(unit);
    }

    private <T extends Uniform> void setUniform(String name, Object value, Map<String, T> uniformMap, boolean strict, boolean array) {
        if (name.isBlank()) throw new IllegalArgumentException("Name must not be empty");

        T uniform = uniformMap.get(name);
        if (uniform != null) {
            setRegisteredUniform(uniform, value, array);
            return;
        }

        if (STRICT_UNIFORM_DETECTION && strict)
            throw new ShaderUniformException("Attempted to set unregistered uniform in strict mode", name);

        if (!unregisteredUniforms.contains(name)) {
            logger.trace("Setting unregistered uniform '{}'", name);
            unregisteredUniforms.add(name);
        }

        setUnregisteredUniform(name, value, array);
    }

    private void setRegisteredUniform(Uniform uniform, Object value, boolean array) {
        if (array) {
            if (uniform instanceof ArrayUniform arrayUniform) {
                if (((Object[]) value).length > arrayUniform.getSize()) {
                    throw new ShaderUniformException("Uniform array value is larger (" + ((Object[]) value).length
                            + ") than uniform array size (" + arrayUniform.getSize() + ")");
                }
            } else {
                throw new ShaderUniformException("Attempted to set non-array value for array uniform: " + value.getClass());
            }
        }

        if (uniform.getType() == Uniform.Type.STRUCT) {
            if (array && value instanceof Object[] values) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] instanceof UniformMappable mappable) {
                        mappable.map(UniformMapper.get(this, uniform.getName() + "[" + i + "]"));
                    } else if (values[i] instanceof Object[]) {
                        throw new IllegalArgumentException("Struct uniform array element was itself an array;" +
                                " likely because of invalid varargs cast");
                    } else {
                        throw new IllegalArgumentException("Struct uniform must implement UniformMappable");
                    }
                }
            } else if (!array && value instanceof UniformMappable mappable) {
                mappable.map(UniformMapper.get(this, uniform.getName()));
            } else {
                throw new ShaderUniformException("Struct uniform must implement UniformMappable");
            }
            return;
        }

        if (array && !uniform.getType().get().equals(((Object[]) value)[0].getClass())
                || !array && !uniform.getType().get().equals(value.getClass())) {
            throw new ShaderUniformException("Uniform " + uniform.getName() + " is present but expected type " +
                    uniform.getType().get().getSimpleName() + " does not match value type " + value.getClass().getSimpleName());
        }

        if (!array) setUniformValue(uniform.getType(), uniform.getLocation(), value);
        else setUniformArrayValue(uniform.getType(), uniform.getLocation(), (Object[]) value);
    }

    private void setUnregisteredUniform(String name, Object value, boolean array) {
        if (value instanceof UniformMappable mappable) { //TODO struct arrays
            mappable.map(UniformMapper.get(this, name));
            return;
        }

        Integer location = nonstrictUniformCache.get(name);
        if (location == null) {
            location = glGetUniformLocation(programID, name);
            nonstrictUniformCache.put(name, location);
        }

        if (location == -1) {
            if (!missingUniforms.contains(name)) {
                logger.debug("Attempted to set nonexistent uniform: {}", name);
                missingUniforms.add(name);
            }
            return;
        }

        if (!array) setUniformValue(Uniform.Type.getFromValue(value), location, value);
        else setUniformArrayValue(Uniform.Type.getFromValue(((Object[]) value)[0]), location, (Object[]) value);
    }

    void setUniformValue(Uniform.Type type, int location, Object value) {
        if (type == null)
            throw new ShaderUniformException("Could not determine uniform type for " + value.getClass().getSimpleName());

        if (AUTO_START_ON_VARIABLE_SET) bind(); //TODO replace with dsa if viable

        try (MemoryStack stack = MemoryStack.stackPush()) {
            switch (type) {
                case INT, SAMPLER2D, SAMPLER_CUBE_MAP, SAMPLER_CUBE_MAP_ARRAY -> glUniform1i(location, (Integer) value);
                case FLOAT -> glUniform1f(location, (Float) value);
                case BOOLEAN -> glUniform1f(location, (boolean) value ? 1 : 0);
                case VEC2 -> glUniform2fv(location, ((Vector2f) value).get(stack.mallocFloat(2)));
                case VEC2I -> glUniform2iv(location, ((Vector2i) value).get(stack.mallocInt(2)));
                case VEC3 -> glUniform3fv(location, ((Vector3f) value).get(stack.mallocFloat(3)));
                case VEC4 -> glUniform4fv(location, ((Vector4f) value).get(stack.mallocFloat(4)));
                case MAT2 -> glUniformMatrix2fv(location, false, ((Matrix2f) value).get(stack.mallocFloat(4)));
                case MAT3 -> glUniformMatrix3fv(location, false, ((Matrix3f) value).get(stack.mallocFloat(9)));
                case MAT4 -> glUniformMatrix4fv(location, false, ((Matrix4f) value).get(stack.mallocFloat(16)));
                default ->
                        throw new IllegalArgumentException("Unknown uniform value type: " + type + " (" + type.get().getSimpleName() + ")");
            }
        }
    }

    void setUniformArrayValue(Uniform.Type type, int location, Object[] value) {
        if (AUTO_START_ON_VARIABLE_SET) bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            switch (type) {
                case INT, SAMPLER2D, SAMPLER_CUBE_MAP -> {
                    IntBuffer ints = stack.mallocInt(value.length);
                    for (Object o : value) ints.put((Integer) o);
                    glUniform1iv(location, ints.rewind());
                }
                case FLOAT -> {
                    FloatBuffer floats = stack.mallocFloat(value.length);
                    for (Object o : value) floats.put((Float) o);
                    glUniform1fv(location, floats.rewind());
                }
                case BOOLEAN -> {
                    IntBuffer ints = stack.mallocInt(value.length);
                    for (Object o : value) ints.put((Boolean) o ? 1 : 0);
                    glUniform1iv(location, ints.rewind());
                }
                case VEC2 -> {
                    FloatBuffer vector2s = stack.mallocFloat(2 * value.length);
                    for (Object o : value) ((Vector2f) o).get(vector2s).position(vector2s.position() + 2);
                    glUniform2fv(location, vector2s.rewind());
                }
                case VEC2I -> {
                    IntBuffer vector2is = stack.mallocInt(2 * value.length);
                    for (Object o : value) ((Vector2i) o).get(vector2is).position(vector2is.position() + 2);
                    glUniform2iv(location, vector2is.rewind());
                }
                case VEC3 -> {
                    FloatBuffer vector3s = stack.mallocFloat(3 * value.length);
                    for (Object o : value) ((Vector3f) o).get(vector3s).position(vector3s.position() + 3);
                    glUniform3fv(location, vector3s.rewind());
                }
                case VEC4 -> {
                    FloatBuffer vector4s = stack.mallocFloat(4 * value.length);
                    for (Object o : value) ((Vector4f) o).get(vector4s).position(vector4s.position() + 4);
                    glUniform4fv(location, vector4s.rewind());
                }
                case MAT2 -> {
                    FloatBuffer matrix2s = stack.mallocFloat(4 * value.length);
                    for (Object o : value) ((Matrix2f) o).get(matrix2s).position(matrix2s.position() + 4);
                    glUniformMatrix2fv(location, false, matrix2s.rewind());
                }
                case MAT3 -> {
                    FloatBuffer matrix3s = stack.mallocFloat(9 * value.length);
                    for (Object o : value) ((Matrix3f) o).get(matrix3s).position(matrix3s.position() + 9);
                    glUniformMatrix3fv(location, false, matrix3s.rewind());
                }
                case MAT4 -> {
                    FloatBuffer matrix4s = stack.mallocFloat(16 * value.length);
                    for (Object o : value) ((Matrix4f) o).get(matrix4s).position(matrix4s.position() + 16);
                    glUniformMatrix4fv(location, false, matrix4s.rewind());
                }
                default ->
                        throw new IllegalArgumentException("Unknown uniform array value type: " + type.get().getSimpleName());
            }
        }
    }

    //TODO if multi-threaded: add thread lock
    //TODO update only if dirty -> push responsibility to UniformMappables?
    public static class UniformMapper {
        private static final UniformMapper instance = new UniformMapper();

        private ShaderProgram shader;
        private String structName;

        static UniformMapper get(ShaderProgram shader, String structName) {
            instance.shader = shader;
            instance.structName = structName;
            return instance;
        }

        private UniformMapper() {
        }

        /**
         * Maps a {@code texture} to a struct uniform sampler named {@code name}. {@code null} values are ignored.
         *
         * @param name    uniform struct member name
         * @param texture texture to sample
         * @return the {@link UniformMapper UniformMapper} for chaining
         */
        public UniformMapper map(String name, @Nullable AbstractTexture texture) {
            if (texture != null) shader.setTexture(structName + "." + name, texture, false);
            return this;
        }

        /**
         * Maps a field {@code value} to a struct uniform member named {@code name}. {@code null} values are ignored.
         *
         * @param name  uniform struct member name
         * @param value uniform value
         * @return the {@link UniformMapper UniformMapper} for chaining
         */
        public UniformMapper map(String name, @Nullable Object value) {
            //TODO strict nested uniforms: probably hook into here for registration
            if (value != null) shader.setUniform(structName + "." + name, value, false);
            return this;
        }
    }

    //TODO move to separate class and replace constructors with factory methods
    @Getter
    public static class Uniform {
        protected static final int INVALID_UNIFORM_LOCATION = -1;
        protected static final int NESTED_UNIFORM_LOCATION = -2;

        private final String name;
        private final Type type;
        private final int location;

        public enum Type {
            INT(Integer.class, () -> 0),
            FLOAT(Float.class, () -> 0f),
            BOOLEAN(Boolean.class, () -> false),
            VEC2(Vector2f.class, Vector2f::new),
            VEC2I(Vector2i.class, Vector2i::new),
            VEC3(Vector3f.class, Vector3f::new),
            VEC4(Vector4f.class, Vector4f::new),
            MAT2(Matrix2f.class, Matrix2f::new),
            MAT3(Matrix3f.class, Matrix3f::new),
            MAT4(Matrix4f.class, Matrix4f::new),

            SAMPLER2D(Integer.class, () -> 0),
            SAMPLER_CUBE_MAP(Integer.class, () -> 0),
            SAMPLER_CUBE_MAP_ARRAY(Integer.class, () -> 0),

            STRUCT(UniformMappable.class, null);

            private final Class<?> type;
            private final Supplier<?> defaultValueGenerator;

            private static final Type[] values = values();

            public static @Nullable Type getFromValue(@Nullable Object value) {
                if (value == null) return null;
                if (value instanceof UniformMappable) return STRUCT;
                for (Type type : values) {
                    if (type.get().equals(value.getClass())) return type;
                }
                return null;
            }

            <T> Type(Class<T> type, Supplier<T> defaultValueGenerator) {
                this.type = type;
                this.defaultValueGenerator = defaultValueGenerator;
            }

            public Class<?> get() {
                return type;
            }

            Object getDefaultValue() {
                return defaultValueGenerator.get();
            }
        }

        public Uniform(String name, Type type, int location) {
            this.name = name;
            this.type = type;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Uniform uniform = (Uniform) o;

            if (!name.equals(uniform.name)) return false;
            return type.equals(uniform.type);
        }

        @Override
        public String toString() {
            return "%s %s".formatted(type.name().toLowerCase(), name);
        }
    }

    @Getter
    protected static class ArrayUniform extends Uniform {
        private final int size;

        public ArrayUniform(String name, int size, Type type, int location) {
            super(name, type, location);
            this.size = size;
        }

        public String getNameWith(int index) {
            if (index > size) throw new ShaderUniformException("Array index out of bounds", super.name);
            return getName().replace("$", Integer.toString(index));
        }

    }

    protected void init() {
    }

    protected abstract String[] getShaderFileNames();

    protected abstract void getUniformLocations();

    protected void addUniform(String name, Uniform.Type type) {
        addUniform(name, type, null);
    }

    protected void addUniform(String name, Uniform.Type type, @Nullable Object defaultValue) {
        if (uniforms.containsKey(name)) return;

        if (type == Uniform.Type.STRUCT) {
            uniforms.put(name, new Uniform(name, type, NESTED_UNIFORM_LOCATION));
            logger.trace("Registered nested uniform '{}'", name);
            return;
        }

        int location = glGetUniformLocation(programID, name);
        if (location != INVALID_UNIFORM_LOCATION) {
            uniforms.put(name, new Uniform(name, type, location));
            if (defaultValue != null) setUniformValue(type, location, defaultValue);
            else setStandardValue(type, location);
            logger.trace("Registered uniform '{}'", name);
            return;
        }

        if (STRICT_UNIFORM_DETECTION)
            throw new ShaderUniformException("Cannot register nonexistent or unused uniform in strict mode", name);

        logger.debug("Could not find uniform '{}' of type {}", name, type);
    }

    protected void addUniformArray(String name, int size, Uniform.Type type) {
        addUniformArray(name, size, type, true);
    }

    protected void addUniformArray(String name, int size, Uniform.Type type, boolean defaultValues) {
        if (arrayUniforms.containsKey(name)) return;

        if (type == Uniform.Type.STRUCT) {
            arrayUniforms.put(name, new ArrayUniform(name, size, type, NESTED_UNIFORM_LOCATION));
            logger.trace("Registered nested array uniform '{}'", name);
            return;
        }

        int location = glGetUniformLocation(programID, name);
        if (location != INVALID_UNIFORM_LOCATION) {
            arrayUniforms.put(name, new ArrayUniform(name, size, type, location));
            if (defaultValues) setStandardArrayValue(type, location, size);
            logger.trace("Registered array uniform '{}'", name);
            return;
        }

        if (STRICT_UNIFORM_DETECTION)
            throw new ShaderUniformException(
                    "Cannot register non-existent or non-used array uniform in strict mode", name);

        logger.debug("Could not find array uniform '{}' of type {}", name, type);
    }

    private void setStandardValue(Uniform.Type type, int location) {
        setUniformValue(type, location, type.getDefaultValue());
    }

    private void setStandardArrayValue(Uniform.Type type, int location, int size) {
        Object[] defaultValues = Stream.generate(type.defaultValueGenerator).limit(size).toArray();
        setUniformArrayValue(type, location, defaultValues);
    }

    protected void disableStrictUniformChecking() {
        this.STRICT_UNIFORM_DETECTION = false;
    }

    protected void hasGeometryShader() {
        this.geometry = true;
    }

    public void checkStatusThrowing() {
        clearError();
        glValidateProgram(programID);
        if (glGetProgrami(programID, GL_VALIDATE_STATUS) != GL_TRUE)
            throw new IllegalStateException("Shader program was not successfully validated\n%s"
                    .formatted(glGetProgramInfoLog(programID)));
        checkErrorThrowing();
    }

    @Override
    public void dispose() {
        stop();
        glDetachShader(programID, vertID);
        glDetachShader(programID, fragID);
        glDeleteProgram(programID);
    }

}
