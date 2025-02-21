package io.github.etieskrill.injection.extension.shaderreflection

import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import org.jetbrains.annotations.VisibleForTesting
import java.io.File

class ShaderReflectionGenerator(private val logger: Logger) {
    fun reflectShaders(
        inputSources: ConfigurableFileTree,
        inputResources: ConfigurableFileTree,
        sourceOutputDir: File,
        resourceOutputDir: File
    ) {
        val annotatedShaders = getAnnotatedShaders(inputSources, inputResources)

        sourceOutputDir.mkdirs()

        val structs = getStructUniforms(annotatedShaders) //TODO generate struct types
        logger.debug("Detected structs: {}", structs)
        val uniforms = getUniforms(annotatedShaders, structs)
        logger.debug("Detected primitive uniforms: {}", uniforms)
        val arrayUniforms = annotatedShaders.associateWith { getArrayUniforms(it, structs[it]) }
        logger.debug("Detected primitive array uniforms: {}", arrayUniforms)

        annotatedShaders.forEach { shader ->
            val sourceOutputBuilder = StringBuilder()

            sourceOutputBuilder.append(
                """
                package ${shader.`package`}

                import io.github.etieskrill.injection.extension.shaderreflection.*
                """.trimIndent() + "\n\n"
            )

            appendPrimitiveUniforms(shader, uniforms[shader], sourceOutputBuilder)
            appendArrayUniforms(shader, arrayUniforms[shader], sourceOutputBuilder)
            appendDirectStructUniforms(shader, structs[shader], sourceOutputBuilder)

            val sourceOutputFile = sourceOutputDir.resolve("${shader.`class`}.kt")
            sourceOutputFile.writeText(sourceOutputBuilder.toString())

            val resourceOutputBuilder = StringBuilder()
            generateUniformResourceFile(uniforms[shader], arrayUniforms[shader], structs[shader], resourceOutputBuilder)

            val resourceOutputFile = resourceOutputDir.resolve("$UNIFORM_RESOURCE_PREFIX${shader.`class`}.csv")
            resourceOutputFile.writeText(resourceOutputBuilder.toString())
        }
    }

    private fun getAnnotatedShaders(
        inputSources: ConfigurableFileTree,
        inputResources: ConfigurableFileTree
    ): List<Shader> {
        return inputSources
            .map {
                it.readText()
                    .split(',')
                    .filter { content -> content.isNotBlank() }
            }
            .map { content ->
                //FIXME import outer classes to discard disgusting accessor property stuff
                val `package` = content[0]
                val `class` = content[1]
                val fullClass = content[2]
                val name = `class`.removeSuffix("Shader")

                val sources = if (content.size > 3) {
                    content.subList(3, content.size)
                        .mapNotNull { source -> inputResources.find { it.name == source } }
                } else {
                    inputResources.filter { it.nameWithoutExtension == name }
                }
                    .map { it.readText() }
                    .map { simplifyGlslSource(it) }

                Shader(name, `package`, fullClass, sources)
            }
    }

    @VisibleForTesting
    internal fun simplifyGlslSource(source: String): String = source
        .run { resolvePreprocessorDirectives(this) }
        .run { removeComments(this) }
        .run { this.lines().joinToString(separator = " ") { it.trim() } }
        .run { removeSuperfluousWhitespace() }

    private fun String.removeSuperfluousWhitespace(): String {
        //FIXME quite ugly, inefficient, and only works because matches are reevaluated every iteration
        val whitespaceRegex = """(?<!\w) +(?=\w)|(?<=\w) +(?!\w)|(?<!\w) +(?!\w)""".toRegex(RegexOption.MULTILINE)

        var source = this
        var match = whitespaceRegex.find(source)
        while (match != null) {
            source = source.replaceRange(match.range, "")
            match = whitespaceRegex.find(source)
        }

        return source
    }

    internal fun removeComments(source: String): String =
        source.run {
            val singleLineCommentRegex = """\/\/[\S\s]*?$""".toRegex(RegexOption.MULTILINE)
            replace(singleLineCommentRegex) { "" }
        }.run {
            val multiLineCommentRegex = """\/\*[\S\s]*?\*\/""".toRegex(RegexOption.MULTILINE)
            replace(multiLineCommentRegex) { "" }
        }

    @VisibleForTesting
    internal fun resolvePreprocessorDirectives(source: String): String {
        var source = resolveDefineDirectives(source)

        val directiveRegex = """#(?<directive>\w+)(?: (?<statement>.*))?""".toRegex(RegexOption.MULTILINE)
        source = source.replace(directiveRegex) { "" }

        return source
    }

    @VisibleForTesting
    internal fun resolveDefineDirectives(source: String): String {
        val defineRegex = """^#define (?<identifier>\w+)(?: (?<statement>[\s\S]+?))?$""".toRegex(RegexOption.MULTILINE)

        var source = source

        var match = defineRegex.find(source)
        while (match != null) {
            source = source.removeRange(match.range)

            val (identifier, statement) = match.destructured
            //TODO exclude keywords, types... fuck
            source = source.replace(identifier, statement)

            match = defineRegex.find(source)
        }

        return source
    }

    private fun getStructUniforms(annotatedShaders: List<Shader>): Map<Shader, List<Struct>> {
        //FIXME man, perhaps a proper lexer/parser setup would be simpler than this
        val structRegex =
            """(?<uniform>uniform )?struct (?<type>\w+)\{(?<content>[ \S]*?)\}(?<instanceNames>\w+(?:\[\w+\])?(?:,\w+(?:\[\w+\])?)*)?;""".toRegex()

        return annotatedShaders.associateWith { shader ->
            shader.sources.flatMap { shaderContent ->
                structRegex
                    .findAll(shaderContent)
                    .map { match ->
                        val members = match.groups["content"]!!.value
                            .split(";")
                            .filter { it.isNotBlank() }
                            .map { member -> member.trim() }
                            .map { it.split(' ') }
                            .associate { it[1] to it[0] }

                        val instanceGroup = match.groups["instanceNames"]?.value
                        val (instances, arrayInstances) = if (!instanceGroup.isNullOrBlank())
                            instanceGroup
                                .split(',')
                                .partition { !it.endsWith("[]") }
                                .let { (instances, arrayInstances) ->
                                    instances to arrayInstances.map {
                                        val arrayStart = it.indexOf('[')
                                        val arrayEnd = it.indexOf(']')
                                        ArrayUniform(
                                            it.substring(0, arrayStart),
                                            "struct",
                                            it.substring(arrayStart + 1, arrayEnd).toInt()
                                        )
                                    }
                                }
                        else null to null

                        return@map Struct(
                            match.groups["type"]!!.value,
                            members,
                            instances,
                            arrayInstances
                        )
                    }
            }
        }
    }

    private fun getUniforms(
        annotatedShaders: List<Shader>,
        structs: Map<Shader, List<Struct>>
    ): Map<Shader, Map<String, String>> {
        //TODO list declarations - for arrays and simple/array mixed lists too...
        val uniformRegex = """uniform (?<type>\w+) (?<instance>\w+);""".toRegex()

        return annotatedShaders.associateWith { shader ->
            shader.sources.flatMap { shaderContent ->
                uniformRegex.findAll(shaderContent).map { match ->
                    var type = match.groups["type"]!!.value
                    if (type in structs[shader]
                            .orEmpty()
                            .map { it.type }
                    )
                        type = "struct"

                    match.groups["instance"]!!.value to type
                }
            }.toMap()
        }
    }

    private fun getArrayUniforms(shader: Shader, structs: List<Struct>?): List<ArrayUniform> {
        val arrayUniformRegex =
            """uniform (?<type>\w+) (?<name>\w+)\[(?<size>\w+)\];""".toRegex()

        //FIXME arrays of samplers do not work, and seem like a fishy concept, and do not even work up to GL4.0
        // according to https://www.reddit.com/r/opengl/comments/10dlhnb/alternative_to_indexing_into_sampler2d_array/

        return shader.sources.flatMap { source ->
            arrayUniformRegex.findAll(source).map { match ->
                var type = match.groupValues[1]
                if (type in structs
                        .orEmpty()
                        .map { it.type }
                )
                    type = "struct"

                val size = match.groups["size"]!!.value
                    .toIntOrNull()
                    ?: run {
                        logger.warn("Could not determine array size for uniform ${match.groups["name"]} in shader $shader")
                        0
                    }

                ArrayUniform(match.groups["name"]!!.value, type, size)
            }
        }
    }

    private fun appendPrimitiveUniforms(shader: Shader, uniforms: Map<String, String>?, outputBuilder: StringBuilder) =
        uniforms?.forEach { (uniformName, uniformType) ->
            outputBuilder.append("val ${shader.`class`}.${uniformName}Name by uniformName()\n")
            outputBuilder.append("var ${shader.`class`}.$uniformName: $uniformType by uniform()\n\n")
        }

    private fun appendArrayUniforms(shader: Shader, arrayUniforms: List<ArrayUniform>?, outputBuilder: StringBuilder) =
        arrayUniforms?.forEach { (name, type, size) ->
            outputBuilder.append("val ${shader.`class`}.${name}Name by uniformName()\n")
            outputBuilder.append("var ${shader.`class`}.$name: Array<$type> by arrayUniform($size)\n\n")
        }

    private fun appendDirectStructUniforms(shader: Shader, structs: List<Struct>?, outputBuilder: StringBuilder) =
        structs
            ?.onEach { (_, _, instances) ->
                instances?.forEach { instance ->
                    outputBuilder.append("val ${shader.`class`}.${instance}Name by uniformName()\n")
                    outputBuilder.append("var ${shader.`class`}.$instance: struct by uniform()\n\n")
                }
            }?.forEach { (_, _, _, arrayInstances) ->
                arrayInstances?.forEach { (name, type, size) ->
                    outputBuilder.append("val ${shader.`class`}.${name}Name by uniformName()\n")
                    outputBuilder.append("var ${shader.`class`}.$name: Array<$type> by arrayUniform($size)\n\n")
                }
            }

    //TODO tests
    private fun generateUniformResourceFile(
        uniforms: Map<String, String>?,
        arrayUniforms: List<ArrayUniform>?,
        structs: List<Struct>?,
        outputBuilder: StringBuilder
    ) {
        uniforms
            ?.map { (name, type) -> "uniform,$type,$name" }
            ?.joinToString("\n")
            ?.let { outputBuilder.append(it).append("\n") }
        arrayUniforms
            ?.joinToString("\n") { (name, type, size) -> "arrayUniform,$type,$size,$name" }
            ?.let { outputBuilder.append(it).append("\n") }
        structs
            ?.forEach { (_, _, names, arrayNames) ->
                names?.forEach { outputBuilder.append("uniform,struct,${it}") }
                arrayNames?.forEach { outputBuilder.append("arrayUniform,struct,${it.size},${it.name}") }
            }
    }
}

data class ArrayUniform(
    val name: String,
    val type: String,
    val size: Int
)

data class Struct(
    val type: String,
    val members: Map<String, String>,
    val instances: List<String>?,
    val arrayInstances: List<ArrayUniform>?
)

data class Shader(
    val name: String,
    val `package`: String,
    val `class`: String,
    val sources: List<String>
) {
    override fun toString(): String = "Shader{name=$name, package=$`package`, class=$`class`}"
}

//TODO some syntactic sugar for kotlin, could be used like:
// shader.uniform.uniform = ...
// shader.setUniform(shader.uniform.uniformName, ...)
//class ShaderUniforms(private val shader: Shader) {
//    val uniformName by uniformName()
//    var uniform
//        get() = shader.uniform
//        set(value) { shader.uniform = value }
//}
//
//val Shader.uniforms: ShaderUniforms
//    get() = ShaderUniforms(this)
