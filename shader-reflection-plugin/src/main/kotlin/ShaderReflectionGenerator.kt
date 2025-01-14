package io.github.etieskrill.injection.extension.shaderreflection

import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import java.io.File

class ShaderReflector(private val logger: Logger) {
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
            generateUniformResourceFile(uniforms[shader], arrayUniforms[shader], resourceOutputBuilder)

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
                }.map { it.readText() } //TODO preprocess to make regexes simpler: resolve preprocessor directives, remove comments, trim indents, remove newlines, remove all spaces unless between keywords/variable identifiers, in that order i think

                Shader(name, `package`, fullClass, sources)
            }
    }

    private fun getStructUniforms(annotatedShaders: List<Shader>): Map<Shader, List<Struct>> {
        //TODO instance name lists
        //FIXME this overflows the struct content to the next match if the struct body does not contain any whitespace, and i, for the life of me, could not fix it. man, perhaps a proper lexer/parser setup would be simpler than this
        val structRegex =
            """(?<uniform>uniform)? *struct(?: +|[\n ]+)(?<type>\w+) *\n*\{\n*(?<content>[\s\S]*?(?!}))[ \n]*} *(?<instanceName>\w+\[?\]?)?;""".toRegex()
        //TODO simplified and corrected struct regex: (?<uniform>uniform )?struct (?<type>\w+)\{(?<content>[ \S]*?)}(?<instanceName>\w+(?:\[\])?)?; multiple comma separated instances are still missing tho

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

                        return@map Struct(
                            match.groups["type"]!!.value,
                            members,
                            match.groups["instanceName"]?.value
                        )
                    }
            }
        }
    }

    private fun getUniforms(
        annotatedShaders: List<Shader>,
        structs: Map<Shader, List<Struct>>
    ): Map<Shader, Map<String, String>> {
        val uniformRegex = """(?<!// *)uniform (\w+) (\w+);""".toRegex()

        return annotatedShaders.associateWith { shader ->
            shader.sources.flatMap { shaderContent ->
                uniformRegex.findAll(shaderContent).map { match ->
                    var type = match.groupValues[1]
                    if (type in structs[shader]
                            .orEmpty()
                            .map { it.type }
                    )
                        type = "struct"

                    match.groupValues[2] to type
                }
            }.toMap()
        }
    }

    private fun getArrayUniforms(shader: Shader, structs: List<Struct>?): List<ArrayUniform> {
        val arrayUniformRegex =
            """uniform[ \n]+(?<type>\w+)[ \n]+(?<name>\w+)[ \n]*\[(?<size>\d+|\w+)\][ \n]*;""".toRegex()

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
                        val defineDirectiveRegex = """^#define (?<name>\w+) (?<value>\d+)\n""".toRegex(RegexOption.MULTILINE)
                        val matches = defineDirectiveRegex.findAll(
                            source.trimIndent() //FIXME find out why this works
                        ).toList()
                        matches
                            .find { it.groups["name"]!!.value == match.groups["size"]!!.value }
                            ?.let {
                                it.groups["value"]!!.value.toIntOrNull() ?: run {
                                    logger.warn("Could not determine array size for uniform ${it.groups["name"]} in shader $shader")
                                    0
                                }
                            } ?: 0
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
            ?.filter { it.instance != null }
            ?.forEach { (_, _, instance) ->
                outputBuilder.append("val ${shader.`class`}.${instance}Name by uniformName()\n")
                outputBuilder.append("var ${shader.`class`}.$instance: struct by uniform()\n\n")
            }

    private fun generateUniformResourceFile(
        uniforms: Map<String, String>?,
        arrayUniforms: List<ArrayUniform>?,
        outputBuilder: StringBuilder
    ) {
        uniforms?.map { (name, type) -> "uniform,$type,$name" }
            ?.joinToString("\n")
            ?.let { outputBuilder.append(it) }
        outputBuilder.append("\n")
        arrayUniforms?.joinToString("\n") { (name, type, size) -> "arrayUniform,$type,$size,$name" }
            ?.let { outputBuilder.append(it) }
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
    val instance: String?
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
