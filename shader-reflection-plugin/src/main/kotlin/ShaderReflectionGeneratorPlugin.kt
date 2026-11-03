package io.github.etieskrill.injection.extension.shader.reflection

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import kotlin.reflect.typeOf

const val GENERATED_MODULE_DIR = "build/generated/shader-reflection/main"
const val GENERATED_SOURCE_DIR = "$GENERATED_MODULE_DIR/kotlin"
const val GENERATED_RESOURCE_DIR = "$GENERATED_MODULE_DIR/resources"

class ShaderReflectionGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
                sourceSets.commonMain.get().apply {
                    kotlin.srcDir(GENERATED_SOURCE_DIR)
                    resources.srcDir(GENERATED_RESOURCE_DIR)
                }
            }

            tasks.apply {
                register("generateShaderReflection", MPPShaderReflectionTask::class.java).configure {
                    it.group = "code generation"
                    it.description = "Generates attribute accessors for shaders annotated with @ReflectShader"
                }

                getByName("build").dependsOn("generateShaderReflection")
            }

            dependencies.apply {
                add("commonMainApi", "io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
                add(
                    "commonMainApi",
                    "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib:1.0.0-SNAPSHOT"
                )
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            extensions.getByType(JavaPluginExtension::class.java).apply {
                sourceSets.getByName("main").apply {
                    java.srcDir(GENERATED_SOURCE_DIR) //FIXME currently detected as source, not generated source ¯\_(ツ)_/¯
                    resources.srcDir(GENERATED_RESOURCE_DIR)
                }
            }

            tasks.apply {
                register("generateShaderReflection", JvmShaderReflectionTask::class.java).configure {
                    it.group = "code generation"
                    it.description = "Generates attribute accessors for shaders annotated with @ReflectShader"
                }

                getByName("compileJava").apply { dependsOn("generateShaderReflection") }
                findByName("compileKotlin")?.apply { dependsOn("generateShaderReflection") }
                getByName("processResources").apply { dependsOn("generateShaderReflection") }
            }

            val dependencyConfig = if (plugins.hasPlugin(JavaLibraryPlugin::class.java)) "api" else "implementation"
            dependencies.apply {
                add(dependencyConfig, "io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
                add(
                    dependencyConfig,
                    "io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib:1.0.0-SNAPSHOT"
                )
            }
        }
    }
}

abstract class JvmShaderReflectionTask : DefaultTask() {
    @get:InputFiles
    val inputResources: ConfigurableFileTree = project.fileTree("src/main/resources").apply {
        include("**/*.glsl", "**/*.vert", "**/*.geom", "**/*.frag")
    }

    @get:InputFiles
    val inputSources: ConfigurableFileTree = project.fileTree("build/generated/ksp/main/resources").apply {
        include("**/$META_FILE_PREFIX*.csv")
    }

    @get:OutputDirectory
    val sourceOutputDir: File = project.file(GENERATED_SOURCE_DIR)

    @get:OutputDirectory
    val resourceOutputDir: File = project.file(GENERATED_RESOURCE_DIR)

    @TaskAction
    fun generateShaderReflections() {
        logger.info("Generating shader reflections for JVM project")
        ShaderReflectionGenerator(logger).reflectShaders(
            inputSources,
            inputResources,
            sourceOutputDir,
            resourceOutputDir
        )
    }
}

abstract class MPPShaderReflectionTask : DefaultTask() {
    @get:InputFiles
    val inputResources: ConfigurableFileTree = project.fileTree("src/commonMain/resources").apply {
        include("**/*.glsl", "**/*.vert", "**/*.geom", "**/*.frag")
    }

    @get:InputFiles
    val inputSources: ConfigurableFileTree = project.fileTree("build/generated/ksp").apply {
        include("*/*Main/resources/**/$META_FILE_PREFIX*.csv")
    }

    @get:OutputDirectory
    val sourceOutputDir: File = project.file(GENERATED_SOURCE_DIR)

    @get:OutputDirectory
    val resourceOutputDir: File = project.file(GENERATED_RESOURCE_DIR)

    @TaskAction
    fun generateShaderReflections() {
        logger.info("Generating shader reflections for multiplatform project")
        ShaderReflectionGenerator(logger).reflectShaders(
            inputSources,
            inputResources,
            sourceOutputDir,
            resourceOutputDir
        )
    }
}
