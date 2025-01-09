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

class ShaderReflector {
    fun reflectShaders(inputSources: ConfigurableFileTree, inputResources: ConfigurableFileTree, outputDir: File) {
        val annotatedShaders = getAnnotatedShaders(inputSources, inputResources)

        outputDir.mkdirs()

        val uniforms = getPrimitiveUniforms(annotatedShaders)
        println("Detected primitive uniform accessors: ${uniforms}")

        uniforms.forEach { (shader, uniforms) ->
            val outputFile = outputDir.resolve("${shader.`class`}.kt")
            outputFile.writeText(
                """
            //Auto-generated
            package ${shader.`package`}
            
            import io.etieskrill.injection.extension.shaderreflection.*
        """.trimIndent() + "\n\n"
            )

            appendPrimitiveUniforms(shader, uniforms, outputFile)
        }
    }

    fun getAnnotatedShaders(inputSources: ConfigurableFileTree, inputResources: ConfigurableFileTree) =
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

    fun getPrimitiveUniforms(annotatedShaders: List<Shader>) =
        annotatedShaders.associate {
            it to it.sources.flatMap { shaderContent ->
                val uniformRegex = """uniform (\w+) (\w+);""".toRegex()

                uniformRegex.findAll(shaderContent).map {
                    it.groupValues[2] to it.groupValues[1]
                }
            }.toMap()
        }

    fun appendPrimitiveUniforms(shader: Shader, uniforms: Map<String, String>, outputFile: File) =
        uniforms.forEach { (uniformName, uniformType) ->
            outputFile.appendText("var ${shader.`class`}.$uniformName: $uniformType by uniform()" + "\n")
        }

    fun getStructUniforms() {

    }
}

data class Shader(
    val name: String,
    val `package`: String,
    val `class`: String,
    val sources: List<String>
) {
    override fun toString(): String = "Shader{name=$name, package=$`package`, class=$`class`}"
}

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
