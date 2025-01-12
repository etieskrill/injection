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

class ShaderAccessorGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        tasks.register("generateShaderReflection", ShaderReflectionTask::class.java).configure {
            group = "code generation"
            description = "Generates attribute accessors for shaders annotated with @ReflectShader"
        }

        val generatedSourceDir = "build/generated/kotlin"

        extensions.configure<JavaPluginExtension>("java") { javaExtension ->
            javaExtension.sourceSets.apply { //FIXME currently detected as source, not generated source ¯\_(ツ)_/¯
                getByName("main").java {
                    it.srcDir(generatedSourceDir)
                }
            }
        }

        plugins.apply("org.jetbrains.kotlin.jvm")

        tasks.apply {
            findByName("compileJava")!!.apply { dependsOn("generateShaderReflection") }
            findByName("compileKotlin")!!.apply { dependsOn("generateShaderReflection") }
        }

        dependencies.add("implementation", "io.github.etieskrill.injection.extension.shaderreflection:shader-reflection-plugin")
    }
}

abstract class ShaderReflectionTask : DefaultTask() {
    private val generatedSourceDir = "build/generated/kotlin"

    @get:InputFiles
    val inputResources: ConfigurableFileTree = project.fileTree("src/main/resources").apply {
        include("**/*.glsl", "**/*.vert", "**/*.geom", "**/*.frag")
    }

    @get:InputFiles
    val inputSources: ConfigurableFileTree = project.fileTree("src/main").apply {
        include("**/*.java", "**/*.kt", "**/*.kts")
    }

    @get:OutputDirectory
    val outputDir: File = project.file(generatedSourceDir)

    @TaskAction
    fun generateShaderReflections() {
        logger.info("Generating shader reflections")
        ShaderReflector(logger).reflectShaders(inputSources, inputResources, outputDir)
    }
}
