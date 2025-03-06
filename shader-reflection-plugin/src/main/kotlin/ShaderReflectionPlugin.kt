package io.github.etieskrill.injection.extension.shader.reflection

import org.gradle.api.Plugin
import org.gradle.api.Project

class ShaderReflectionPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.run {
            apply(ShaderReflectionGeneratorPlugin::class.java)
            apply(ShaderReflectionProcessorPlugin::class.java)
        }
    }
}