package org.etieskrill.engine.graphics.shader

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.StorageBufferObject
import org.etieskrill.engine.graphics.buffer.gl
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.gl.shader.ShaderCreationException
import org.etieskrill.engine.graphics.shader.UniformType.*
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.util.ResourceReader.getResource
import org.etieskrill.engine.util.extension
import org.joml.Matrix2f
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.putMatrix2f
import org.joml.putMatrix3f
import org.joml.putMatrix4f
import org.joml.putVector2f
import org.joml.putVector2i
import org.joml.putVector3f
import org.joml.putVector4f
import org.lwjgl.opengl.GL11C.GL_TRUE
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.opengl.GL31C.GL_INVALID_INDEX
import org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER
import org.lwjgl.opengl.GL40C.glUniform1d
import org.lwjgl.opengl.GL40C.glUniform1dv
import org.lwjgl.opengl.GL43C.glGetProgramResourceIndex
import org.lwjgl.system.MemoryStack

private val logger = KotlinLogging.logger {}

//TODO instance-ize needs to only synchronise uniforms and stuff when bound
/**
 * A shader file with the _glsl_ extension is presumed to contain exactly a vertex shader, a fragment shader
 * and - if the rudimentary detection catches it - a geometry shader within the corresponding definition guards.
 */
internal actual class ShaderInstance internal constructor(
    actual val descriptor: Shader,
    actual val context: GraphicsContext,
) : Disposable {

    private var programId: Int = -1
    private var vertId: Int = -1
    private var geomId: Int = -1
    private var fragId: Int = -1

    private val uniforms = mutableMapOf<String, UniformInstance>()
    private val arrayUniforms = mutableMapOf<String, UniformArrayInstance>()
    private var version: Long = 0L

    private val boundTextures = mutableSetOf<Texture>()
    private val cachedStorageBuffers = mutableMapOf<String, Int>()

    private data class UniformInstance(val uniform: Uniform, val location: Int, var version: Long = 0L)
    private data class UniformArrayInstance(val uniform: ArrayUniform, val location: Int, var version: Long = 0L)

    init {
        val files = descriptor.shaderFiles.map { fileName ->
            val type = when (fileName.extension) {
                "vert" -> ShaderType.VERTEX
                "geom" -> ShaderType.GEOMETRY
                "frag" -> ShaderType.FRAGMENT
                "glsl" -> ShaderType.COMPOSITE
                else -> throw ShaderCreationException("Cannot load shader with unknown file extension: $fileName")
            }
            ShaderFile(fileName, type, getResource(fileName))
        }.toSet()

        if (files.size == 1 && !files.all { it.type == ShaderType.COMPOSITE }) {
            throw ShaderCreationException("Single-file shaders must have 'glsl' extension")
        } else if (files.size != 1 && !files
                .map { it.type }
                .toSet()
                .containsAll(setOf(ShaderType.VERTEX, ShaderType.FRAGMENT))
        ) {
            throw ShaderCreationException("Shader must have one vertex and one fragment shader")
        }
        //TODO create spec for other shader types and shader programs split across multiple files
        // consider just creating a standard, such that only a single unique identifier must be passed

        logger.debug { "Creating shader from files: $files" }

        createShader(files)

        if (GLUtils.checkError("OpenGL error during shader creation")) {
            logger.info { "Successfully created shader" }
        }
    }

    private fun createShader(files: Set<ShaderFile>) {
        GLUtils.clearError()
        if (files.size > 1) createProgram(files)
        else createSingleFileProgram(files.single())

//        bind()
        //TODO move to front-end
        //TODO find and add uniforms from files
        //TODO filter and warn on duplicates, prefer config, then UniformEntries, then autodetected
//        val uniformFileName = UNIFORM_RESOURCE_PREFIX + ClassUtils.getFullName(this) + ".csv"
//        if (classpathResourceExists(uniformFileName)) {
//            getResource(uniformFileName)
//                .lines()
//                .filter { !it.isBlank() }
//                .map { it.split(",") }
//                .forEach { uniformTypeName ->
//                    val type = UniformType.entries.find { it.glslName == uniformTypeName[1] }
//                    if (type == null) {
//                        throw ShaderCreationException("Unsupported uniform type: ${uniformTypeName[1]}")
//                    }
//                    when (uniformTypeName[0]) {
//                        "uniform" -> addUniform(
//                            uniformTypeName[2],
//                            type.clazz
//                        ) //TODO parse structs in separate branch when generated types are introduced
//                        "arrayUniform" -> addUniformArray(
//                            uniformTypeName[3],
//                            Integer.parseInt(uniformTypeName[2]),
//                            type.clazz
//                        )
//                    }
//                }
//        }

//        unbind()
    }

    private fun createProgram(files: Set<ShaderFile>) {
        programId = glCreateProgram()

        vertId = loadShader(files, ShaderType.VERTEX)
        glAttachShader(programId, vertId)

        if (files.any { it.type == ShaderType.GEOMETRY }) {
            geomId = loadShader(files, ShaderType.GEOMETRY)
            glAttachShader(programId, geomId)
        }

        fragId = loadShader(files, ShaderType.FRAGMENT)
        glAttachShader(programId, fragId)

        linkProgram()
    }

    private fun createSingleFileProgram(file: ShaderFile) {
        programId = glCreateProgram()

        vertId = loadShader(file, ShaderType.VERTEX)
        glAttachShader(programId, vertId)
        if (checkForGeometryShader(file.source)) {
            try {
                geomId = loadShader(file, ShaderType.GEOMETRY)
                glAttachShader(programId, geomId)
            } catch (e: ShaderCreationException) {
                throw ShaderCreationException("Failed to compile geometry shader from composite shader file", e)
            }
        }
        fragId = loadShader(file, ShaderType.FRAGMENT)
        glAttachShader(programId, fragId)

        linkProgram()
    }

    //TODO betterify by resolving pragmas earlier
    private fun checkForGeometryShader(source: String): Boolean =
        source.contains("#ifdef GEOMETRY_SHADER")
                || source.contains("#pragma stage geometry")
                || source.contains("#pragma stage geom")

    private fun linkProgram() {
        glLinkProgram(programId)
        if (glGetProgrami(programId, GL_LINK_STATUS) != GL_TRUE)
            throw ShaderCreationException("Shader program could not be linked", glGetProgramInfoLog(programId))

        disposeShaders()

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

        GLUtils.checkError("Error while creating shader")
    }

    private fun loadShader(files: Set<ShaderFile>, type: ShaderType): Int = loadShader(
        files.find { it.type == type }
            ?: throw ShaderCreationException("No ${type.name.lowercase()} shader file was found"),
        type)

    //TODO shader binary caching
//    var supposedLength = BufferUtils.createIntBuffer(1);
//    GLUtils.clearError();
//    GL41C.glGetProgramiv(programID, GL_PROGRAM_BINARY_LENGTH, supposedLength);
//    GLUtils.checkErrorThrowing();
//    System.out.println("Supposed length: " + supposedLength.get());
//
//    IntBuffer length = BufferUtils.createIntBuffer(1), format = BufferUtils.createIntBuffer(1);
//    var data = BufferUtils.createByteBuffer(supposedLength.get(0));
//    GLUtils.clearError();
//    GL41C.glGetProgramBinary(programID, length, format, data);
//    GLUtils.checkErrorThrowing("Failed to retrieve shader binary");
//    System.out.println("Length: " + length.get() + ", format: " + format.get());
//    try {
//        //TODO finger Path#register
//        char[] charData = new char[length.get(0)];
//        for (int i = 0; i < charData.length; i++) {
//            charData[i] = data.getChar();
//        }
//        var file = Path.of("./succ.bin").toFile();
//        if (!file.exists()) file.createNewFile();
//        var writer = new FileWriter(file);
//        writer.write(charData);
//        writer.close();
//    } catch (IOException e) {
//        throw new RuntimeException(e);
//    }
//
//    GL41C.glShaderBinary(new int[]{programID}, format.get(0), data.rewind());

    //TODO add loader for shader objects and wrap calls to this method in said loader
    private fun loadShader(file: ShaderFile, type: ShaderType): Int {
        logger.trace { "Loading ${file.type.name.lowercase()} shader from file: $file" }

        val shaderId = glCreateShader(
            when (type) {
                ShaderType.VERTEX -> GL_VERTEX_SHADER
                ShaderType.GEOMETRY -> GL_GEOMETRY_SHADER
                ShaderType.FRAGMENT -> GL_FRAGMENT_SHADER
                else -> error("Unexpected value: $type")
            }
        )

        //TODO use #line <nr> to improve debugging experience

        var shaderSource = file.source
        if (file.type == ShaderType.COMPOSITE) {
            shaderSource = resolveShaderStagePragmaDirectives(shaderSource)
            //TODO filter for missing required directives - and add default?
            shaderSource = injectShaderCompileDirective(shaderSource, type)
        }
        //TODO cache compiled shaders - and even reuse program objects, and only change uniforms via uniform buffers?
        glShaderSource(shaderId, shaderSource)
        glCompileShader(shaderId)

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) != GL_TRUE) {
            throw ShaderCreationException(
                "Failed to compile ${file.type.name.lowercase()} shader from $file", glGetShaderInfoLog(shaderId)
            )
        }

        return shaderId
    }

    private fun resolveShaderStagePragmaDirectives(shaderSource: String): String = buildString {
        var currentStage = ShaderStage.NONE
        val stageMatcher = Regex("#pragma stage (\\w+)", RegexOption.IGNORE_CASE)

        for (line in shaderSource.lines()) {
            val matches = stageMatcher.findAll(line, 0).iterator()

            if (!matches.hasNext()) {
                append(line).append("\n")
                continue
            }

            if (currentStage != ShaderStage.NONE) {
                append("#endif\n")
            }

            currentStage = getNextStage(matches)

            if (currentStage == ShaderStage.NONE) continue

            append("#ifdef ").append(currentStage.name).append("_SHADER\n")
        }

        if (currentStage != ShaderStage.NONE) {
            append("#endif\n")
        }
    }

    private fun getNextStage(matches: Iterator<MatchResult>): ShaderStage {
        var lastMatch: MatchResult? = null
        while (matches.hasNext()) lastMatch =
            matches.next() //we only want the last directive in the line //FIXME what???
        return when (lastMatch?.groupValues[1]?.lowercase()) {
            "none" -> ShaderStage.NONE
            "vert", "vertex" -> ShaderStage.VERTEX
            "geom", "geometry" -> ShaderStage.GEOMETRY
            "frag", "fragment" -> ShaderStage.FRAGMENT
            else -> throw ShaderCreationException("Unexpected shader stage value: ${lastMatch?.groupValues[1]}")
        }
    }

    private fun injectShaderCompileDirective(shaderSource: String, type: ShaderType): String {
        val shaderSourceLines = shaderSource.lines().toMutableList()
        val firstNonBlankLine = shaderSourceLines.indexOfFirst { !it.isBlank() }
        shaderSourceLines.add(firstNonBlankLine + 1, "#define ${type.name}_SHADER")
        return shaderSourceLines.joinToString("\n")
    }

    private fun disposeShaders() {
        glDeleteShader(vertId)
        if (geomId != -1) glDeleteShader(geomId)
        glDeleteShader(fragId)
    }

    fun bind() = context.withContext {
        if (context.activeShader != this) {
            glUseProgram(programId)
            context.activeShader = this
        }

        syncUniforms()
    }

    fun unbind() = context.withContext {
        if (context.activeShader != null) {
            glUseProgram(0)
            context.activeShader = null
        }
    }

    private fun syncUniforms() {
        if (version < descriptor.version) {
            descriptor.uniforms.forEach { (name, uniform) ->
                if (name in uniforms) return@forEach

                val location = glGetUniformLocation(programId, name)
                if (location == -1) {
                    logger.error { "Failed to get uniform $name in shader ${descriptor::class.simpleName}" }
                    return@forEach
                }

                uniforms[name] = UniformInstance(uniform, location)
            }

            descriptor.uniformArrays.forEach { (name, uniform) ->
                if (name in uniforms) return@forEach

                val location = glGetUniformLocation(programId, name)
                if (location == -1) {
                    logger.error { "Failed to get array uniform $name in shader ${descriptor::class.simpleName}" }
                    return@forEach
                }

                arrayUniforms[name] = UniformArrayInstance(uniform, location)
            }

            version = descriptor.version
        }

        uniforms.values.forEach { uniform ->
            val descriptorUniform = uniform.uniform
            if (uniform.version < descriptorUniform.version) {
                val value = descriptorUniform.value

                if (value != null) {
                    when (descriptorUniform.type) {
                        INT, FLOAT, DOUBLE, BOOL, VEC2, VEC2I, VEC3, VEC4, MAT2, MAT3, MAT4 -> {
                            setUniformValue(descriptorUniform.type, uniform.location, value)
                        }

                        TEXTURE_2D, TEXTURE_2D_ARRAY, TEXTURE_2D_SHADOW, TEXTURE_2D_ARRAY_SHADOW, TEXTURE_CUBE_MAP,
                        TEXTURE_CUBE_MAP_SHADOW, TEXTURE_CUBE_MAP_ARRAY, TEXTURE_CUBE_MAP_ARRAY_SHADOW -> {
                            val texture = value as Texture

                            val unit = context.textureBindings.indexOfFirst { it == texture }.takeIf { it != -1 }
                                ?: context.textureBindings.indexOf(null).takeIf { it != -1 }
                                ?: context.textureBindings.indexOfFirst { it?.descriptor !in boundTextures }
                                    .takeIf { it != -1 }
                                ?: error("oopsie daisy")

                            boundTextures += texture
                            setUniformValue(descriptorUniform.type, uniform.location, unit)
                            context.getTexture(texture).bind(unit)
                        }

                        STORAGE_BUFFER -> {
                            val storageBuffer = context.getStorageBufferObject(value as StorageBufferObject<*>)

                            val bindingIndex = cachedStorageBuffers.computeIfAbsent(descriptorUniform.name) {
                                glGetProgramResourceIndex(
                                    programId,
                                    storageBuffer.descriptor.type.gl,
                                    descriptorUniform.name
                                )
                            }
                            if (bindingIndex == GL_INVALID_INDEX) {
                                logger.error { "Failed to get binding index for storage buffer ${descriptorUniform.name}" }
                            } else {
                                storageBuffer.bind(bindingIndex)
                            }
                        }

                        STRUCT -> {}
                    }
                }

                uniform.version = descriptorUniform.version
            }
        }

        arrayUniforms.values.forEach { uniform ->
            val descriptorUniform = uniform.uniform
            if (uniform.version < descriptorUniform.version) {
                descriptorUniform.value?.let {
                    setUniformArrayValue(
                        descriptorUniform.elementType,
                        uniform.location,
                        it
                    )
                }
                uniform.version = descriptorUniform.version
            }
        }
    }

    private fun setUniformValue(type: UniformType, location: Int, value: Any) {
        bind()
        MemoryStack.stackPush().use { stack ->
            when (type) {
                INT, TEXTURE_2D, TEXTURE_2D_ARRAY, TEXTURE_2D_SHADOW, TEXTURE_2D_ARRAY_SHADOW, TEXTURE_CUBE_MAP,
                TEXTURE_CUBE_MAP_ARRAY, TEXTURE_CUBE_MAP_SHADOW, TEXTURE_CUBE_MAP_ARRAY_SHADOW ->
                    glUniform1i(location, value as Int)

                FLOAT -> glUniform1f(location, value as Float)
                DOUBLE -> glUniform1d(location, value as Double)
                BOOL -> glUniform1i(location, if (value as Boolean) 1 else 0) //FIXME or f?
                VEC2 -> glUniform2fv(location, (value as Vector2f).get(stack.mallocFloat(2)))
                VEC2I -> glUniform2iv(location, (value as Vector2i).get(stack.mallocInt(2)))
                VEC3 -> glUniform3fv(location, (value as Vector3f).get(stack.mallocFloat(3)))
                VEC4 -> glUniform4fv(location, (value as Vector4f).get(stack.mallocFloat(4)))
                MAT2 -> glUniformMatrix2fv(location, false, (value as Matrix2f).get(stack.mallocFloat(4)))
                MAT3 -> glUniformMatrix3fv(location, false, (value as Matrix3f).get(stack.mallocFloat(9)))
                MAT4 -> glUniformMatrix4fv(location, false, (value as Matrix4f).get(stack.mallocFloat(16)))
                STORAGE_BUFFER -> {}
                STRUCT -> error("Struct types should have been resolved by here already")
            }
        }
    }

    private fun setUniformArrayValue(type: UniformType, location: Int, value: Array<Any>) {
        bind()
        MemoryStack.stackPush().use { stack ->
            when (type) {
                INT, TEXTURE_2D, TEXTURE_2D_ARRAY, TEXTURE_2D_SHADOW, TEXTURE_2D_ARRAY_SHADOW, TEXTURE_CUBE_MAP,
                TEXTURE_CUBE_MAP_ARRAY, TEXTURE_CUBE_MAP_SHADOW, TEXTURE_CUBE_MAP_ARRAY_SHADOW -> {
                    val ints = stack.mallocInt(value.size)
                    for (o in value) ints.put(o as Int)
                    glUniform1iv(location, ints.rewind())
                }

                FLOAT -> {
                    val floats = stack.mallocFloat(value.size)
                    for (o in value) floats.put(o as Float)
                    glUniform1fv(location, floats.rewind())
                }

                DOUBLE -> {
                    val doubles = stack.mallocDouble(value.size)
                    for (o in value) doubles.put(o as Double)
                    glUniform1dv(location, doubles.rewind())
                }

                BOOL -> {
                    val ints = stack.mallocInt(value.size)
                    for (o in value) ints.put(if (o as Boolean) 1 else 0)
                    glUniform1iv(location, ints.rewind())
                }

                VEC2 -> {
                    val vector2s = stack.mallocFloat(2 * value.size)
                    value.forEachIndexed { i, vec2 -> vector2s.putVector2f(2 * i, vec2 as Vector2f) }
                    glUniform2fv(location, vector2s.rewind())
                }

                VEC2I -> {
                    val vector2is = stack.mallocInt(2 * value.size)
                    value.forEachIndexed { i, vec2i -> vector2is.putVector2i(2 * i, vec2i as Vector2i) }
                    for (o in value) (o as Vector2i)[vector2is].position(vector2is.position() + 2)
                    glUniform2iv(location, vector2is.rewind())
                }

                VEC3 -> {
                    val vector3s = stack.mallocFloat(3 * value.size)
                    value.forEachIndexed { i, vec3 -> vector3s.putVector3f(3 * i, vec3 as Vector3f) }
                    glUniform3fv(location, vector3s.rewind())
                }

                VEC4 -> {
                    val vector4s = stack.mallocFloat(4 * value.size)
                    value.forEachIndexed { i, vec4 -> vector4s.putVector4f(4 * i, vec4 as Vector4f) }
                    glUniform4fv(location, vector4s.rewind())
                }

                MAT2 -> {
                    val mat2s = stack.mallocFloat(4 * value.size)
                    value.forEachIndexed { i, mat2 -> mat2s.putMatrix2f(4 * i, mat2 as Matrix2f) }
                    glUniformMatrix2fv(location, false, mat2s.rewind())
                }

                MAT3 -> {
                    val mat3s = stack.mallocFloat(9 * value.size)
                    value.forEachIndexed { i, mat3 -> mat3s.putMatrix3f(9 * i, mat3 as Matrix3f) }
                    glUniformMatrix3fv(location, false, mat3s.rewind())
                }

                MAT4 -> {
                    val mat4s = stack.mallocFloat(16 * value.size)
                    value.forEachIndexed { i, mat4 -> mat4s.putMatrix4f(16 * i, mat4 as Matrix4f) }
                    glUniformMatrix4fv(location, false, mat4s.rewind())
                }

                STORAGE_BUFFER -> {}
                STRUCT -> error("Struct types should have been resolved by here already")
            }
        }
    }

    actual override fun dispose() = context.withContext {
        unbind()
//        glDetachShader(programId, vertId) // glDeleteProgram does all of this already by spec
//        glDeleteShader(vertId)
//        if (geomId != -1) {
//            glDetachShader(programId, geomId)
//            glDeleteShader(geomId)
//        }
//        glDetachShader(programId, fragId)
//        glDeleteShader(fragId)
        glDeleteProgram(programId)
    }

}
