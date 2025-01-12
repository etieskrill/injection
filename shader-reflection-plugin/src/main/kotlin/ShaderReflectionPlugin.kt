package io.github.etieskrill.injection.extension.shaderreflection

import org.gradle.api.Plugin
import org.gradle.api.Project

class ShaderReflectionPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply("com.google.devtools.ksp")

        //TODO add as api if target has `java-library`
        dependencies.add("implementation", "io.github.etieskrill.injection.extension.shaderreflection:shader-interface")
        dependencies.add("implementation", "io.github.etieskrill.injection.extension.shaderreflection:shader-reflection-plugin")

        pluginManager.apply(ShaderAccessorGeneratorPlugin::class.java)
    }
}