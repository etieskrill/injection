package io.github.etieskrill.injection.extension.shaderreflection

import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import java.io.File

class ShaderReflector(private val logger: Logger) {
    fun reflectShaders(inputSources: ConfigurableFileTree, inputResources: ConfigurableFileTree, outputDir: File) {
        val annotatedShaders = getAnnotatedShaders(inputSources, inputResources)

        outputDir.mkdirs()

        val structs = getStructUniforms(annotatedShaders) //TODO generate struct types
        val uniforms = getUniforms(annotatedShaders, structs)
        logger.info("Detected primitive uniforms: $uniforms")
        val arrayUniforms = annotatedShaders
            .associateWith { getArrayUniforms(it, structs[it]) }

        uniforms.forEach { (shader, uniforms) ->
            val outputFile = outputDir.resolve("${shader.`class`}.kt")
            outputFile.writeText(
                """
            package ${shader.`package`}

            import io.github.etieskrill.injection.extension.shaderreflection.*
        """.trimIndent() + "\n\n"
            )

            appendPrimitiveUniforms(shader, uniforms, outputFile)
            appendArrayUniforms(shader, arrayUniforms[shader], outputFile)
            appendDirectStructUniforms(shader, structs[shader], outputFile)
        }
    }

    private fun getAnnotatedShaders(inputSources: ConfigurableFileTree, inputResources: ConfigurableFileTree) =
        inputSources
            .map { it to it.readText() }
            .filter { (_, content) -> content.contains("@ReflectShader") } //TODO with param extraction TODO restrict to classes implementing ShaderProgram (which may be renamed to Shader mayhaps)
            .map { (file, content) ->
                Shader(
                    file.nameWithoutExtension.removeSuffix("Shader"),
                    """package ([\w\.]+[^;\s])""".toRegex().find(
                        content.lines().first()
                    )!!.groupValues[1], //FIXME you don't put "documentation" in front of the package statement, right?
                    file.nameWithoutExtension,
                    inputResources
                        .filter { file.nameWithoutExtension.removeSuffix("Shader") == it.nameWithoutExtension }
                        .map { it.readText() }
                )
            }

    private fun getStructUniforms(annotatedShaders: List<Shader>): Map<Shader, List<Struct>> {
        //TODO instance name lists
        //FIXME this overflows the struct content to the next match if the struct body does not contain any whitespace, and i, for the life of me, could not fix it. man, perhaps a proper lexer/parser setup would be simpler than this
        val structRegex =
            """(?<uniform>uniform)? *struct(?: +|[\n ]+)(?<type>\w+) *\n*\{\n*(?<content>[\s\S]*?(?!}))[ \n]*} *(?<instanceName>\w+\[?\]?)?;""".toRegex()

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
        val uniformRegex = """uniform (\w+) (\w+);""".toRegex()

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
                    ?: run { //FIXME see e.g. DepthCubeMapArrayShader.kt - does not work
                        val defineDirectiveRegex = """^#define (?<name>\w+) (?<value>\d+)\n""".toRegex()
                        val matches = defineDirectiveRegex.findAll(source).toList()
                        matches
                            .find { it.groups["name"]!!.value == match.groups["size"]!!.value }
                            ?.let { it.groups["value"]!!.value.toIntOrNull() ?: run {
                                logger.warn("Could not determine array size for uniform ${it.groups["name"]} in shader $shader")
                                0
                            } } ?: 0
                    }

                ArrayUniform(match.groups["name"]!!.value, type, size)
            }
        }
    }

    private fun appendPrimitiveUniforms(shader: Shader, uniforms: Map<String, String>, outputFile: File) =
        uniforms.forEach { (uniformName, uniformType) ->
            outputFile.appendText("val ${shader.`class`}.${uniformName}Name by uniformName()\n")
            outputFile.appendText("var ${shader.`class`}.$uniformName: $uniformType by uniform()\n\n")
        }

    private fun appendArrayUniforms(shader: Shader, arrayUniforms: List<ArrayUniform>?, outputFile: File) =
        arrayUniforms?.forEach { (name, type, size) ->
            outputFile.appendText("val ${shader.`class`}.${name}Name by uniformName()\n")
            outputFile.appendText("var ${shader.`class`}.$name: Array<$type> by arrayUniform($size)\n\n")
        }

    private fun appendDirectStructUniforms(shader: Shader, structs: List<Struct>?, outputFile: File) {
        structs
            ?.filter { it.instance != null }
            ?.forEach { (_, _, instance) ->
                outputFile.appendText("val ${shader.`class`}.${instance}Name by uniformName()\n")
                outputFile.appendText("var ${shader.`class`}.$instance: struct by uniform()\n\n")
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
