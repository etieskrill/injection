package io.github.etieskrill.injection.extension.shader.reflection

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ShaderReflectionProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.apply("com.google.devtools.ksp")

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            dependencies.add(
                "ksp", "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-plugin"
            )
            tasks.getByName("generateShaderReflection").dependsOn("kspKotlin")

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

            kotlin.targets.configureEach { target ->
                val targetName = target.name.replaceFirstChar { it.uppercase() }
                configurations.matching { it.name == "ksp$targetName" }
                    .all { // live collections, eh? not an obscure api AT ALL
                        dependencies.add(
                            "ksp$targetName",
                            "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-plugin"
                        )
                        tasks.getByName("generateShaderReflection").dependsOn("kspKotlin$targetName")
                    }
            }

            kotlin.sourceSets.configureEach {
                it.dependencies {
                    implementation("io.github.etieskrill.injection.extension.shader:shader-interface")
                    implementation("io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib")
                }
            }
        }
    }
}
