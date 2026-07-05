package io.github.etieskrill.injection.extension.shader.reflection

import com.google.devtools.ksp.gradle.KspGradleSubplugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ShaderReflectionProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply(KspGradleSubplugin::class.java)

        dependencies.add("ksp", "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-plugin")

        tasks.getByName("generateShaderReflection").dependsOn("kspKotlin")

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val dependencyConfig = if (plugins.hasPlugin(JavaLibraryPlugin::class.java)) "api" else "implementation"
            dependencies.apply {
                add(dependencyConfig, "io.github.etieskrill.injection.extension.shader:shader-interface")
                add(
                    dependencyConfig,
                    "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib"
                )
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

            kotlin.sourceSets.configureEach {
                it.dependencies {
                    implementation("io.github.etieskrill.injection.extension.shader:shader-interface")
                    implementation("io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib")
                }
            }
        }
    }
}
