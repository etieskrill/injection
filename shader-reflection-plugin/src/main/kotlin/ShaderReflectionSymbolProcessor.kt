package io.github.etieskrill.injection.extension.shaderreflection

//import com.google.devtools.ksp.processing.*
//import com.google.devtools.ksp.symbol.KSAnnotated

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
//            dependencies = Dependencies.ALL_FILES,
//            packageName = "io.github.etieskrill.injection.extension.shaderreflection",
//            fileName = "shader-reflection-meta",
//            extensionName = "csv"
//        )
//
//        shaderReflectionMetaFile.use { output ->
//            output.write("""
//                yoyoyo dis sum mad shite
//            """.trimIndent().toByteArray())
//        }
//
//        return emptyList()
//    }
//}