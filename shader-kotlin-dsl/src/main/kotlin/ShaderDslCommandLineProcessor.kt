package io.github.etieskrill.injection.extension.shader.dsl

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

//to my understanding, the command line processor only acts as an api to pass options between the gradle plugin and the compiler plugin
@OptIn(ExperimentalCompilerApi::class)
internal class ShaderDslCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = GEN_RESOURCE_DIR_NAME,
            valueDescription = "Generated resource directory",
            description = "Path to generated resource directory"
        )
    )

    companion object {
        const val GEN_RESOURCE_DIR_NAME = GEN_RESOURCE_DIR_ARG_NAME
        val GEN_RESOURCE_DIR_KEY = CompilerConfigurationKey.create<String>(GEN_RESOURCE_DIR_NAME)
    }

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        when (option.optionName) {
            GEN_RESOURCE_DIR_NAME -> configuration[GEN_RESOURCE_DIR_KEY] = value

            else -> error("Unexpected config option ${option.optionName}")
        }

}

private operator fun <T : Any> CompilerConfiguration.set(key: CompilerConfigurationKey<T>, value: T) = put(key, value)
