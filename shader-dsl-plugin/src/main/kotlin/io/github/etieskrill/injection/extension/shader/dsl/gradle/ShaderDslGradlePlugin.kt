package io.github.etieskrill.injection.extension.shader.dsl.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

internal const val PLUGIN_ID = "injShaderDslPlugin"
internal const val GEN_RESOURCE_DIR_ARG_NAME = "resourceOutputDir"
internal const val GEN_RESOURCE_DIR = "build/generated/shader-dsl/main/resources"

//the gradle plugin, which specifies the gradle-sided config and passes some project-related info to the compiler plugin
@Suppress("unused")
internal class ShaderDslGradlePlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val GROUP_ID = "io.github.etieskrill.injection.extension.shader.dsl"
        const val ARTIFACT_ID = "shader-dsl-plugin"
        const val VERSION = "1.0.0-SNAPSHOT"
    }

    //TODO try defining in/outputs here

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val resourceDir = project.fileTree(GEN_RESOURCE_DIR).dir

        return project.provider { //these options are passed to the command line processor
            listOf(
                FilesSubpluginOption(GEN_RESOURCE_DIR_ARG_NAME, listOf(resourceDir))
            )
        }
    }

    override fun apply(target: Project): Unit = target.run {
        val dependencyConfig = if (plugins.hasPlugin(JavaLibraryPlugin::class.java)) "api" else "implementation"
        dependencies.apply {
            add(dependencyConfig, "$GROUP_ID:shader-dsl-lib")
            add(dependencyConfig, "$GROUP_ID.std:shader-dsl-std-lib")
        }

        extensions.configure<JavaPluginExtension>("java") { javaExtension ->
            javaExtension.sourceSets.getByName("main").apply {
                resources.srcDir(GEN_RESOURCE_DIR)
            }
        }

        tasks.named("compileKotlin") {
            it.outputs.dir(GEN_RESOURCE_DIR)
                .withPropertyName("shaderDslResources")
        }

//        tasks.named("processResources") {
//            it.inputs.dir(GEN_RESOURCE_DIR)
//                .withPropertyName("shaderDslResources")
//        }

        //FIXME this isn't great for caching, but the above does not work???
        tasks.named("processResources") { it.mustRunAfter("compileKotlin") }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(GROUP_ID, ARTIFACT_ID, VERSION)

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

}
