package io.github.etieskrill.injection.extension.shader.reflection

import com.google.devtools.ksp.gradle.KspGradleSubplugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class ShaderReflectionProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply(KspGradleSubplugin::class.java)

        dependencies.add("ksp", "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-plugin")

        tasks.getByName("generateShaderReflection").dependsOn("kspKotlin")

        dependencies.apply {
            add("implementation", "io.github.etieskrill.injection.extension.shader:shader-interface")
            add("implementation", "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-plugin")
        }
    }
}