package io.github.etieskrill.injection.extension.shaderreflection

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

const val GENERATED_MODULE_DIR = "build/generated/shader-reflection/main"
const val GENERATED_SOURCE_DIR = "$GENERATED_MODULE_DIR/kotlin"
const val GENERATED_RESOURCE_DIR = "$GENERATED_MODULE_DIR/resources"

class ShaderReflectionGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        tasks.register("generateShaderReflection", ShaderReflectionTask::class.java).configure {
            group = "code generation"
            description = "Generates attribute accessors for shaders annotated with @ReflectShader"
        }

        extensions.configure<JavaPluginExtension>("java") { javaExtension ->
            javaExtension.sourceSets.getByName("main").apply {
                java.srcDir(GENERATED_SOURCE_DIR) //FIXME currently detected as source, not generated source ¯\_(ツ)_/¯
                resources.srcDir(GENERATED_RESOURCE_DIR)
            }
        }

        plugins.apply("org.jetbrains.kotlin.jvm")

        tasks.apply {
            getByName("compileJava").apply { dependsOn("generateShaderReflection") }
            getByName("compileKotlin").apply { dependsOn("generateShaderReflection") }
            getByName("processResources").apply { dependsOn("generateShaderReflection") }
        }

        //TODO add as api if target has `java-library`?
        dependencies.apply {
            add("implementation", "io.github.etieskrill.injection.extension.shaderreflection:shader-interface")
            add("implementation", "io.github.etieskrill.injection.extension.shaderreflection:shader-reflection-plugin")
        }
    }
}

abstract class ShaderReflectionTask : DefaultTask() {
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
        logger.info("Generating shader reflections")
        ShaderReflectionGenerator(logger).reflectShaders(
            inputSources,
            inputResources,
            sourceOutputDir,
            resourceOutputDir
        )
    }
}
