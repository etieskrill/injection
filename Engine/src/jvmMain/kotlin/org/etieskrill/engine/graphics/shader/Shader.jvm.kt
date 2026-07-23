package org.etieskrill.engine.graphics.shader

import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import io.github.etieskrill.injection.extension.shader.Texture as DslTexture
import io.github.etieskrill.injection.extension.shader.reflection.UNIFORM_RESOURCE_PREFIX
import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.buffer.StorageBufferObject
import org.etieskrill.engine.graphics.buffer.gl
import org.etieskrill.engine.graphics.gl.GLUtils
import org.etieskrill.engine.graphics.gl.shader.ShaderCreationException
import org.etieskrill.engine.graphics.gl.shader.ShaderUniformException
import org.etieskrill.engine.graphics.shader.Uniform.Companion.INVALID_UNIFORM_LOCATION
import org.etieskrill.engine.graphics.shader.UniformType.*
import org.etieskrill.engine.graphics.texture.Texture
import org.etieskrill.engine.util.ClassUtils
import org.etieskrill.engine.util.ResourceReader.classpathResourceExists
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
import org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20C.GL_LINK_STATUS
import org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20C.glAttachShader
import org.lwjgl.opengl.GL20C.glCompileShader
import org.lwjgl.opengl.GL20C.glCreateProgram
import org.lwjgl.opengl.GL20C.glCreateShader
import org.lwjgl.opengl.GL20C.glDeleteProgram
import org.lwjgl.opengl.GL20C.glDeleteShader
import org.lwjgl.opengl.GL20C.glGetProgramInfoLog
import org.lwjgl.opengl.GL20C.glGetProgrami
import org.lwjgl.opengl.GL20C.glGetShaderInfoLog
import org.lwjgl.opengl.GL20C.glGetShaderi
import org.lwjgl.opengl.GL20C.glGetUniformLocation
import org.lwjgl.opengl.GL20C.glLinkProgram
import org.lwjgl.opengl.GL20C.glShaderSource
import org.lwjgl.opengl.GL20C.glUniform1f
import org.lwjgl.opengl.GL20C.glUniform1fv
import org.lwjgl.opengl.GL20C.glUniform1i
import org.lwjgl.opengl.GL20C.glUniform1iv
import org.lwjgl.opengl.GL20C.glUniform2fv
import org.lwjgl.opengl.GL20C.glUniform2iv
import org.lwjgl.opengl.GL20C.glUniform3fv
import org.lwjgl.opengl.GL20C.glUniform4fv
import org.lwjgl.opengl.GL20C.glUniformMatrix2fv
import org.lwjgl.opengl.GL20C.glUniformMatrix3fv
import org.lwjgl.opengl.GL20C.glUniformMatrix4fv
import org.lwjgl.opengl.GL20C.glUseProgram
import org.lwjgl.opengl.GL31C.GL_INVALID_INDEX
import org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER
import org.lwjgl.opengl.GL40C.glUniform1d
import org.lwjgl.opengl.GL40C.glUniform1dv
import org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BLOCK
import org.lwjgl.opengl.GL43C.glGetProgramResourceIndex
import org.lwjgl.system.MemoryStack
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

/**
 * A shader file with the _glsl_ extension is presumed to contain exactly a vertex shader, a fragment shader
 * and - if the rudimentary detection catches it - a geometry shader within the corresponding definition guards.
 */
actual abstract class Shader protected actual constructor(
    actual val context: GraphicsContext,
    shaderFiles: List<String>,
    private val strictUniformDetection: Boolean
) : AbstractShader, Disposable {

    private var programId: Int = -1
    private var vertId: Int = -1
    private var geomId: Int = -1
    private var fragId: Int = -1

    private val uniforms = mutableMapOf<String, Uniform>()
    private val arrayUniforms = mutableMapOf<String, ArrayUniform>()
    private val storageBuffers = mutableMapOf<String, Int>()

    private val boundTextures = mutableSetOf<Texture>()

    private val missingStorageBuffers = mutableSetOf<String>()

    private val cachedUnstrictUniforms = mutableMapOf<String, Uniform>()
    private val cachedUnstrictArrayUniforms = mutableMapOf<String, ArrayUniform>()
    private val cachedUnstrictStorageBuffers = mutableMapOf<String, Int>()

    init {
        val files = shaderFiles.map { fileName ->
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

        bind()
        //TODO find and add uniforms from files
        //TODO filter and warn on duplicates, prefer config, then UniformEntries, then autodetected
        val uniformFileName = UNIFORM_RESOURCE_PREFIX + ClassUtils.getFullName(this) + ".csv"
        if (classpathResourceExists(uniformFileName)) {
            getResource(uniformFileName)
                .lines()
                .filter { !it.isBlank() }
                .map { it.split(",") }
                .forEach { uniformTypeName ->
                    val type = UniformType.entries.find { it.glslName == uniformTypeName[1] }
                    if (type == null) {
                        throw ShaderCreationException("Unsupported uniform type: ${uniformTypeName[1]}")
                    }
                    when (uniformTypeName[0]) {
                        "uniform" -> addUniform(uniformTypeName[2], type.clazz) //TODO parse structs in separate branch when generated types are introduced
                        "arrayUniform" -> addUniformArray(uniformTypeName[3], Integer.parseInt(uniformTypeName[2]), type.clazz)
                    }
                }
        }

        unbind()
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

        val shaderId = glCreateShader(when (type) {
            ShaderType.VERTEX -> GL_VERTEX_SHADER
            ShaderType.GEOMETRY -> GL_GEOMETRY_SHADER
            ShaderType.FRAGMENT -> GL_FRAGMENT_SHADER
            else -> error("Unexpected value: $type")
        });

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
        while (matches.hasNext()) lastMatch = matches.next() //we only want the last directive in the line //FIXME what???
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
    }

    fun unbind() = context.withContext {
        if (context.activeShader != null) {
            glUseProgram(0)
            context.activeShader = null
        }
    }

    actual override fun setUniform(name: String, value: Any) {
        val uniform = if (strictUniformDetection) {
            uniforms[name] ?: error("Tried setting unregistered uniform '$name' in strict mode")
        } else {
            cachedUnstrictUniforms.getOrPut(name) {
                val type = UniformType.entries.find { it.clazz == value::class || it.constClass == value::class }
                    ?: error("Could not recognise as GLSL type: ${value::class.simpleName}")
                Uniform(name, type, glGetUniformLocation(programId, name))
            }
        }

        setUniformValue(uniform.type, uniform.location, value)
    }

    //TODO validate index
    actual override fun setUniformArray(name: String, index: Int, value: Any) = setUniform("$name[$index]", value)

    actual override fun setUniformArray(name: String, value: Array<Any>) {
        if (value.isEmpty()) return

        //TODO validate size

        val uniform = if (strictUniformDetection) {
            arrayUniforms[name] ?: error("Tried setting unregistered array uniform '$name' in strict mode")
        } else {
            cachedUnstrictArrayUniforms.getOrPut(name) {
                val type = UniformType.entries.find { it.clazz == value[0]::class || it.constClass == value[0]::class }
                    ?: error("Could not recognise as GLSL type: ${value[0]::class.simpleName}")
                ArrayUniform(name, type, value.size, glGetUniformLocation(programId, name))
            }
        }

        setUniformArrayValue(uniform.type, uniform.location, value)
    }

    /**
     * Binds a [Texture] to a shader's uniform sampler called `name` if there are still texture slots available. A
     * maximum of [GraphicsContext.maxTextureUnits] can be bound in a single shader, which is hardware-dependent.
     *
     * This method first tries to find if [texture] is already bound to a texture unit, and only sets the uniform in
     * that case, then tries to find a vacant binding point, and otherwise overrides a slot that is not occupied by a
     * texture this shader needs.
     *
     * @param name    the sampler name in the shader
     * @param texture the texture to be bound
     */
    actual override fun setTexture(name: String, texture: DslTexture) = context.withContext {
        texture as Texture //TODO is there a non-workaroundy way to do this?

        if (boundTextures.size + 1 > context.maxTextureUnits) {
            logger.error { "Could not bind texture to '$name' because there are already the platform maximum of ${boundTextures.size} textures bound" }
        }

        val unit = context.textureBindings.indexOfFirst { it == texture }.takeIf { it != -1 }
            ?: context.textureBindings.indexOf(null).takeIf { it != -1 }
            ?: context.textureBindings.indexOfFirst { it !in boundTextures }.takeIf { it != -1 }
            ?: error("oopsie daisy")

        //TODO validate texture type

        setUniform(name, unit)
        texture.bind(unit)
    }

    //FIXME these are nonlocal to the shader, and should thus probably be cached and activated in start
    actual override fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>) = context.withContext {
        buffer as StorageBufferObject

        check(blockName.isNotBlank()) { "Name must not be empty" }

        val bindingIndex = if (strictUniformDetection) {
            storageBuffers[blockName] ?: error("Tried setting unregistered storage buffer '${blockName}' in strict mode")
        } else {
            cachedUnstrictStorageBuffers.computeIfAbsent(blockName) {
                glGetProgramResourceIndex(programId, buffer.type.gl, blockName)
            }
        }

        if (bindingIndex == GL_INVALID_INDEX && blockName !in missingStorageBuffers) {
            logger.warn { "Storage buffer block '$blockName' does not exist" }
            missingStorageBuffers += blockName
        }
        if (context.storageBufferBindings[bindingIndex] != buffer) {
            buffer.bind(bindingIndex)
        }
    }

    private fun setUniformValue(type: UniformType, location: Int, value: Any) = context.withContext {
        //TODO check if cpu/gpu values in sync already
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
                STRUCT -> error("Struct types should have been resolved by here already")
            }
        }
    }

    private fun setUniformArrayValue(type: UniformType, location: Int, value: Array<Any>) = context.withContext {
        //TODO check if cpu/gpu values in sync already
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

                STRUCT -> error("Struct types should have been resolved by here already")
            }
        }
    }

    actual override fun addUniform(name: String, type: KClass<*>) {
        if (uniforms.containsKey(name)) return //can only be registered if valid in actual shader, thus no redefinition

        val uniformType = UniformType.entries.find { it.clazz == type || it.constClass == type }
            ?: error("Could not recognise as GLSL type: ${type.simpleName}")

        if (uniformType == STRUCT) {
            uniforms[name] = Uniform(name, uniformType, Uniform.NESTED_UNIFORM_LOCATION)
            logger.trace { "Registered uniform struct '$name'" }
            return
        }

        val location = context.withContext { glGetUniformLocation(programId, name) }
        if (location != INVALID_UNIFORM_LOCATION) {
            uniforms[name] = Uniform(name, uniformType, location)
            logger.trace { "Registered uniform '$name'" }
            return
        }

        if (strictUniformDetection) {
            throw ShaderUniformException("Cannot register nonexistent or unused uniform in strict mode", name)
        }

        logger.debug { "Could not find uniform '$name' of type $uniformType" }
    }

    actual override fun addUniformArray(name: String, size: Int, type: KClass<*>) {
        if (arrayUniforms.containsKey(name)) return //can only be registered if valid in actual shader, thus no redefinition

        val uniformType = UniformType.entries.find { it.clazz == type || it.constClass == type }
            ?: error("Could not recognise as GLSL type: ${type.simpleName}")

        //TODO validate size

        if (uniformType == STRUCT) {
            arrayUniforms[name] = ArrayUniform(name, uniformType, size, Uniform.NESTED_UNIFORM_LOCATION)
            logger.trace { "Registered uniform struct array '$name'" }
            return
        }

        val location = context.withContext { glGetUniformLocation(programId, name) }
        if (location != INVALID_UNIFORM_LOCATION) {
            arrayUniforms[name] = ArrayUniform(name, uniformType, size, location)
            logger.trace { "Registered uniform array '$name'" }
            return
        }

        if (strictUniformDetection) {
            throw ShaderUniformException("Cannot register nonexistent or unused array uniform in strict mode", name)
        }

        logger.debug { "Could not find array uniform '$name' of type $uniformType" }
    }

    actual override fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>) {
        if (blockName in storageBuffers) return

        val bindingIndex = context.withContext {
            glGetProgramResourceIndex(programId, GL_SHADER_STORAGE_BLOCK, blockName)
        }
        if (bindingIndex != GL_INVALID_INDEX) {
            storageBuffers[blockName] = bindingIndex
            logger.trace { "Registered storage buffer binding index $bindingIndex with name '$blockName'" }
            return
        }

        if (strictUniformDetection) {
            throw ShaderUniformException("Cannot register inexistent or unused storage block '$blockName' in strict mode")
        }

        logger.warn { "Storage buffer binding index with name '$blockName' not found" }
    }

    actual override fun dispose() {
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

internal actual open class Uniform(
    actual val name: String,
    actual val type: UniformType,
    internal val location: Int
) {
    companion object {
        internal const val INVALID_UNIFORM_LOCATION = -1
        internal const val NESTED_UNIFORM_LOCATION = -2
    }

    //FIXME equals without location?
}

internal actual class ArrayUniform(
    name: String,
    type: UniformType,
    actual val size: Int,
    location: Int
) : Uniform(name, type, location)
