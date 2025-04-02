package io.github.etieskrill.injection.extension.shader.dsl

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import java.io.File

//this is the entrypoint to register all compiler extensions and pass whatever config and needed env components to them
@OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)
internal class ShaderDslCompilerPlugin : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val genResourcePath = configuration[ShaderDslCommandLineProcessor.GEN_RESOURCE_DIR_KEY]!!
        val genResourceDir = File(genResourcePath)
        if (!genResourceDir.exists()) {
            genResourceDir.mkdirs()
        }

        val compilerOptions = ShaderDslCompilerOptions(genResourceDir)

        IrGenerationExtension.registerExtension(
            IrShaderGenerationExtension(compilerOptions, configuration.messageCollector)
        )
    }
}

//TODO add generated file name options
internal data class ShaderDslCompilerOptions(
    val generatedResourceDir: File
)
