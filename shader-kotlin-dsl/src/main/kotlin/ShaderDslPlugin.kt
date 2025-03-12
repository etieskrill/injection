package io.github.etieskrill.injection.extension.shader.dsl

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.*

@Suppress("unused")
class ShaderDslPlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val SERIALIZATION_GROUP_NAME = "io.github.etieskrill.injection.extension.shader.dsl"
        const val ARTIFACT_ID = "shader-kotlin-dsl"
        const val VERSION = "1.0.0-SNAPSHOT"
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider {
            listOf() //TODO look into the SubPlugin thingamajig
        }
    }

    override fun apply(target: Project): Unit = target.run {
        dependencies.apply {
            add("implementation", "io.github.etieskrill.injection.extension.shader.dsl:shader-kotlin-dsl")
        }
    }

    override fun getCompilerPluginId(): String = "shaderKotlinDslPlugin"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, ARTIFACT_ID, VERSION)

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}

@OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)
class IrShaderGenerationRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            IrShaderGenerationExtension(configuration.messageCollector)
        )
    }
}

class IrShaderGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    @OptIn(ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val shaderClasses = moduleFragment
            .files
            .map { it.declarations }
            .flatten()
            .filterIsInstance<IrClass>()
            .filter { it.superClass != null && "${it.superClass?.packageFqName?.asString()}.${it.superClass?.name?.asString()}" == ShaderBuilder::class.qualifiedName }

        val programBodies = shaderClasses
            .first()
            .findDeclaration<IrFunction> { it.name.asString() == "program" }!!
            .body!!
            .statements

        programBodies
            .map { it.dump().log() } //TODO hell ye brother, now we're cooking with fire
    }

    fun Any?.log() = messageCollector.report(CompilerMessageSeverity.ERROR, "$this")
}
