package io.github.etieskrill.injection.extension.shader.dsl

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

const val TASK_NAME = "generateGlslStuff"

class ShaderDslPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
//        tasks.register(TASK_NAME, ShaderDslPsiParserTask::class.java) {
//            group = "code generation"
//            description = "Generates GLSL code for programs in ShaderBuilder classes"
//        }
//
//        plugins.apply("org.jetbrains.kotlin.jvm")
//
//        tasks.apply {
//            getByName("compileJava").dependsOn(TASK_NAME)
//            getByName("compileKotlin").dependsOn(TASK_NAME)
//            //TODO perhaps processResources as well
//        }

        dependencies.apply {
//            add("ksp", "io.github.etieskrill.injection.extension.shader.dsl:shader-kotlin-dsl")
            add("implementation", "io.github.etieskrill.injection.extension.shader.dsl:shader-kotlin-dsl")
        }
    }
}

@OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)
class IrShaderGenerationRegistrar : CompilerPluginRegistrar() {
//    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
//        IrGenerationExtension.registerExtension(
//            project,
//            IrShaderGenerationExtension(configuration.messageCollector)
//        )
//    }

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            IrShaderGenerationExtension(configuration.messageCollector)
        )
    }
}

//class IrShaderGenerationExtension(
//    private val messageCollector: MessageCollector
//) : IrGenerationExtension {
//    @OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
//    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
//        //TODO omg this actually bloody worked... how to target the module the plugin is applied in tho?
//        messageCollector.report(CompilerMessageSeverity.ERROR, moduleFragment.files.first().declarations.map {
//            when (it) {
//                is IrClass -> it.name
//                else -> it.descriptor.name.asString()
//            }
//        }.joinToString())
//
//        //TODO ig this could be moved to just the plugin as well
//        // either search for ShaderBuilder class or use ksp somehow to find shader builders
//        val environment = KotlinCoreEnvironment.Companion.createForProduction(
//            { println("Disposed, i guess") },
//            CompilerConfiguration.EMPTY,
//            EnvironmentConfigFiles.JVM_CONFIG_FILES
//        )
//
//        val language = Language.findLanguageByID("kotlin")!!
//        val psiFile = PsiFileFactory.getInstance(environment.project).createFileFromText(language, file)
//
//        val builder = StringBuilder(psiFile.node.toString())
//        builder.append(psiFile.children.toString())
//        messageCollector.report(CompilerMessageSeverity.ERROR, builder.toString())
//
////        messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, moduleFragment.toString())
////        messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, pluginContext.toString())
////        messageCollector.report(CompilerMessageSeverity.ERROR, "WOOOOO")
//    }
//}
