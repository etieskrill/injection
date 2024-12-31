tasks.register<ShaderReflectionTask>("generateShaderReflection") {
    group = "code generation"
    description = "Generates attribute accessors for shaders annotated with @ReflectShader"
}

tasks.named("compileKotlin") { dependsOn("generateShaderReflection") }

val generatedSourceDir = "build/generated/kotlin"

the<SourceSetContainer>().forEach {
    println("${it.name}, ${it.allSource}")
}

abstract class ShaderReflectionTask : DefaultTask() {
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
        val annotatedShaderNames = inputSources
            .filter { it.readText().contains("@ReflectShader") } //TODO with param extraction
            .map { it.nameWithoutExtension.removeSuffix("Shader") }

        val shaderContents = annotatedShaderNames
            .map { shaderName -> shaderName to inputResources.filter { shaderName == it.nameWithoutExtension } }
            .associate { (name, files) -> name to files.map { it.readText() } }

        val uniforms = shaderContents.map { (shaderName, shaderContents) ->
            shaderName to shaderContents.flatMap { shaderContent ->
                val uniformRegex = """uniform (\w+) (\w+);""".toRegex()

                uniformRegex.findAll(shaderContent).map {
                    it.groupValues[2] to it.groupValues[1]
                }
            }.toMap()
        }.toMap()

        outputDir.mkdirs()

        uniforms.forEach { (shaderName, uniforms) ->
            val outputFile = outputDir.resolve("${shaderName}Shader.kt")//.apply { createNewFile() }
            outputFile.writeText("""
                //Auto-generated
                //package $ originalPackage
            """.trimIndent() + "\n\n")

            uniforms.forEach { (uniformName, uniformType) ->
                outputFile.appendText("""
                    var ${shaderName}Shader.$uniformName: $uniformType
                        get() = TODO()
                        set(value) = setUniform("$uniformName", value)
                """.trimIndent() + "\n\n")
            }
        }

        logger.warn(uniforms.toString())
        println("what the sigma")
    }
}

//class Uniform<T : Any>(val name: String) {
//    operator fun getValue(singleColourShader: SingleColourShader, property: KProperty<*>): T = TODO()//singleColourShader.getUniform()
//    operator fun setValue(singleColourShader: SingleColourShader, property: KProperty<*>, value: T) = singleColourShader.setUniform(name, value)
//}
