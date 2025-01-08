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

data class Shader(
    val name: String,
    val `package`: String,
    val `class`: String,
    val sources: List<String>
) {
    override fun toString(): String = "Shader{name=$name, package=$`package`, class=$`class`}"
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
        val annotatedShaders = inputSources
            .map { it to it.readText() }
            .filter { (_, content) -> content.contains("@ReflectShader") } //TODO with param extraction TODO restrict to classes implementing ShaderProgram (which may be renamed to Shader mayhaps)
            .map { (file, content) -> Shader(
                file.nameWithoutExtension.removeSuffix("Shader"),
                """package ([\w\.]+[^;\s])""".toRegex().find(content.lines().first())!!.groupValues[1], //FIXME you don't put "documentation" in front of the package statement, right?
                file.nameWithoutExtension,
                inputResources
                    .filter { file.nameWithoutExtension.removeSuffix("Shader") == it.nameWithoutExtension }
                    .map { it.readText() }
            ) }

        val uniforms = annotatedShaders.map {
            it to it.sources.flatMap { shaderContent ->
                val uniformRegex = """uniform (\w+) (\w+);""".toRegex()

                uniformRegex.findAll(shaderContent).map {
                    it.groupValues[2] to it.groupValues[1]
                }
            }.toMap()
        }.toMap()
        println("Generating uniform accessors: ${uniforms}")

        outputDir.mkdirs()

        uniforms.forEach { (shader, uniforms) ->
            val outputFile = outputDir.resolve("${shader.`class`}.kt")//.apply { createNewFile() }
            outputFile.writeText("""
                //Auto-generated
                package ${shader.`package`}
                
                import io.etieskrill.injection.extension.shaderreflection.*
            """.trimIndent() + "\n\n")

            uniforms.forEach { (uniformName, uniformType) ->
                outputFile.appendText("var ${shader.`class`}.$uniformName: $uniformType by uniform()" + "\n")
            }
        }
    }
}
