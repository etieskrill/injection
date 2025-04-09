@file:OptIn(ExperimentalCompilerApi::class)

package io.github.etieskrill.injection.extension.shader.dsl

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

class ShaderTest {

    @Test
    fun `Should build simple top-level direct superclass shader builder`() {
        val result = compile(getSource("SimpleTestShader.kt"))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(getGeneratedResource("SimpleTest.glsl"))
            .isEqualToIgnoringWhitespace(getResource("SimpleTest.glsl"))
    }

    @Test
    fun `Should build shader builder with constants and if conditionals`() {
        val result = compile(getSource("HDRShader.kt"))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(getGeneratedResource("HDR.glsl"))
            .isEqualToIgnoringWhitespace(getResource("HDR.glsl"))
    }

    @Test
    fun `Should build shader builder with inline if conditionals`() {
        val result = compile(getSource("HDRShader2.kt"))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(getGeneratedResource("HDRShader2.glsl"))
            .isEqualToIgnoringWhitespace(getResource("HDRShader2.glsl"))
    }

    @Test
    fun `Should build shader builder with inline when conditionals`() {
        val result = compile(getSource("HDRShader3.kt"))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(getGeneratedResource("HDRShader3.glsl"))
            .isEqualToIgnoringWhitespace(getResource("HDR.glsl"))
    }

    @Test
    fun `Should build shader builder with function calls and basic for loops`() {
        val result = compile(getSource("GaussBlurShader.kt"))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        assertThat(getGeneratedResource("GaussBlur.glsl"))
            .isEqualToIgnoringWhitespace(getResource("GaussBlur.glsl"))
    }

    //TODO
    // - supertype type param resolving
    // - ensure required stages
    // - ensure num stages
    // - ensure writeability (uniforms writeable or smth)
    // - ensure only program stages in program function

    @AfterTest
    fun cleanup() {
//        val generatedFolder = File(TEST_RESOURCE_TARGET)
//        if (generatedFolder.exists()) generatedFolder.deleteRecursively()
    }

}

const val TEST_SRC_ROOT = "src/test/kotlin"

fun getSource(fileName: String): SourceFile {
    val path = "data/$fileName"
    val file = File(TEST_SRC_ROOT, path)

    if (!file.exists()) error("File $path does not exist")

    return SourceFile.kotlin(fileName, file.readText())
}

fun compile(file: SourceFile) =
    KotlinCompilation().apply {
        sources = listOf(file/*, SourceFile.kotlin("ShaderDsl.kt", File("src/main/kotlin/ShaderDsl.kt").readText())*/)

        compilerPluginRegistrars = listOf(ShaderDslCompilerPlugin())

        val processor = ShaderDslCommandLineProcessor()
        commandLineProcessors = listOf(processor)
        pluginOptions = listOf(
            processor.option(
                ShaderDslCommandLineProcessor.GEN_RESOURCE_DIR_NAME,
                "build/generated/shader-dsl/main/resources"
            )
        )

        inheritClassPath = true
//        kotlincArguments = listOf("-Xcontext-receivers")

        verbose = false
    }.compile()

fun CommandLineProcessor.option(key: String, value: Any?) =
    PluginOption(pluginId, key, value.toString())

const val TEST_RESOURCE_ROOT = "src/test/resources/shaders"

fun getResource(fileName: String): String {
    val file = File(TEST_RESOURCE_ROOT, fileName)
    if (!file.exists()) error("File $file does not exist")
    return file.readText()
}

const val TEST_RESOURCE_TARGET = "build/generated/shader-dsl/main/resources/shaders"

fun getGeneratedResource(fileName: String): String {
    val file = File(TEST_RESOURCE_TARGET, fileName)
    assertThat(file).exists().isNotEmpty()
    return file.readText()
}
