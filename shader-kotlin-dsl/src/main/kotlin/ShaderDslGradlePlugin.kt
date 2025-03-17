package io.github.etieskrill.injection.extension.shader.dsl

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

internal const val PLUGIN_ID = "injShaderDslPlugin"
internal const val GEN_RESOURCE_DIR_ARG_NAME = "resourceOutputDir"
internal const val GEN_RESOURCE_DIR = "build/generated/shader-dsl/main/resources"

//the gradle plugin, which specifies the gradle-sided config and passes some project-related info to the compiler plugin
internal class ShaderDslGradlePlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val GROUP_ID = "io.github.etieskrill.injection.extension.shader.dsl"
        const val ARTIFACT_ID = "shader-kotlin-dsl"
        const val VERSION = "1.0.0-SNAPSHOT"
    }

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
        dependencies.apply {
            add("implementation", "$GROUP_ID:$ARTIFACT_ID")
        }

        extensions.configure<JavaPluginExtension>("java") { javaExtension ->
            javaExtension.sourceSets.getByName("main").apply {
                resources.srcDir(GEN_RESOURCE_DIR)
            }
        }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(GROUP_ID, ARTIFACT_ID, VERSION)

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

}
