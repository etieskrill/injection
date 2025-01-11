package io.github.etieskrill.injection

tasks.register<ShaderReflectionTask>("generateShaderReflection") {
    group = "code generation"
    description = "Generates attribute accessors for shaders annotated with @ReflectShader"
}

val generatedSourceDir = "build/generated/kotlin"

extensions.configure<JavaPluginExtension>("java") {
    sourceSets {
        getByName("main") {
            java.srcDir(generatedSourceDir)
        }
    }
}

plugins.apply("org.jetbrains.kotlin.jvm")

tasks {
    named("compileJava") { dependsOn("generateShaderReflection") }
    named("compileKotlin") { dependsOn("generateShaderReflection") }
}

dependencies {
    "implementation"(project(":shader-reflection"))
}

abstract class ShaderReflectionTask : DefaultTask() {
    private val generatedSourceDir = "build/generated/kotlin"

    @get:InputFiles
    val inputResources = project.fileTree("src/main/resources").apply {
        include("**/*.glsl", "**/*.vert", "**/*.geom", "**/*.frag")
    }

    @get:InputFiles
    val inputSources = project.fileTree("src/main").apply {
        include("**/*.java", "**/*.kt", "**/*.kts")
    }

    @get:OutputDirectory
    val outputDir = project.file(generatedSourceDir)

    @TaskAction
    fun generateShaderReflections() {
        ShaderReflector().reflectShaders(inputSources, inputResources, outputDir)
    }
}

//-------------------------- THIS SHIT HATES BEING IN ANOTHER FILE -----------------------------

//TODO get uniforms if not manually specified - add name and type as map to generated accessor file
//TODO add uniform block class to shader for ease of access
//TODO uniform arrays

class ShaderReflector {
    fun reflectShaders(inputSources: ConfigurableFileTree, inputResources: ConfigurableFileTree, outputDir: File) {
        val annotatedShaders = getAnnotatedShaders(inputSources, inputResources)

        outputDir.mkdirs()

        val structs = getStructUniforms(annotatedShaders) //TODO generate struct types
        val uniforms = getUniforms(annotatedShaders, structs)
        println("Detected primitive uniforms: $uniforms")

        uniforms.forEach { (shader, uniforms) ->
            val outputFile = outputDir.resolve("${shader.`class`}.kt")
            outputFile.writeText(
                """
            package ${shader.`package`}
            
            import io.etieskrill.injection.extension.shaderreflection.*
        """.trimIndent() + "\n\n"
            )

            appendPrimitiveUniforms(shader, uniforms, outputFile)
            appendDirectStructUniforms(shader, structs[shader].orEmpty(), outputFile)
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
        //FIXME this overflows the struct content to the next match if the struct body does not contain any whitespace, and i, for the life of me, could not fix it. man, perhaps a proper lexer/parser setup would be simpler than this
        val structRegex =
            """(?<uniform>uniform)? *struct(?: +|[\n ]+)(?<type>\w+) *\n*\{\n*(?<content>[\s\S]*?(?!}))[ \n]*} *(?<instanceName>\w+)?;""".toRegex()

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
                    ) {
                        type = "struct"
                    }

                    match.groupValues[2] to type
                }
            }.toMap()
        }
    }

    private fun appendPrimitiveUniforms(shader: Shader, uniforms: Map<String, String>, outputFile: File) =
        uniforms.forEach { (uniformName, uniformType) ->
            outputFile.appendText("val ${shader.`class`}.${uniformName}Name by uniformName()\n")
            outputFile.appendText("var ${shader.`class`}.$uniformName: $uniformType by uniform()\n\n")
        }

    private fun appendDirectStructUniforms(shader: Shader, structs: List<Struct>, outputFile: File) {
        structs
            .filter { it.instance != null }
            .forEach { (_, _, instance) ->
                outputFile.appendText("val ${shader.`class`}.${instance}Name by uniformName()\n")
                outputFile.appendText("var ${shader.`class`}.$instance: struct by uniform()\n\n")
            }
    }
}

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

//------------------------------------------ KSP ------------------------------------------

//plugins {
//    id("com.google.devtools.ksp")
//}

//dependencies {
//    "ksp"(project("io.github.etieskrill.injection:shaderreflection"))
//}
//
//class ShaderReflectorSymbolProcessorProvider : SymbolProcessorProvider {
//    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
//        ShaderReflectorSymbolProcessor(environment.codeGenerator, environment.logger)
//}
//
//class ShaderReflectorSymbolProcessor(
//    private val generator: CodeGenerator,
//    private val logger: KSPLogger
//) : SymbolProcessor {
//    override fun process(resolver: Resolver): List<KSAnnotated> {
//        val symbols = resolver.getSymbolsWithAnnotation("ReflectShader")
//
//        logger.warn(symbols.toString())
//
//        val shaderReflectionMetaFile = generator.createNewFile(
//            dependencies = com.google.devtools.ksp.processing.Dependencies.Companion.ALL_FILES,
//            packageName = "io.github.etieskrill.injection.extension.shaderreflection",
//            fileName = "shader-reflection-meta",
//            extensionName = "csv"
//        )
//
//        shaderReflectionMetaFile.use { output ->
//            output.write("""
//                yoyoyo dis sum mat shite
//            """.trimIndent().toByteArray())
//        }
//
//        return emptyList()
//    }
//}
