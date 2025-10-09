plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "injection.sandbox"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    implementation(platform("org.lwjgl:lwjgl-bom:${libs.versions.lwjgl.get()}")) //TODO figure out platforms & catalogs
    implementation(libs.lwjgl.openal)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.stb)
    //natives not specified as they are brought in by the engine

    implementation("io.github.etieskrill.injection.extension.shader.dsl:shader-dsl-plugin:1.0.0-SNAPSHOT")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.register<Exec>("compileShaders") {
    inputs.file("src/main/kotlin/shader.kt")

    val classpath = configurations.runtimeClasspath.get()
        .filter { jar -> listOf("kotlin-stdlib", "shader-interface", "shader-dsl-lib", "shader-dsl-std-lib", "joml").any {
            jar.name.contains(it)
        } }
        .joinToString(";")

    val pluginClasspath = configurations.runtimeClasspath.get()
        .filter { jar -> listOf("shader-dsl-plugin", "shader-interface", "shader-dsl-lib", "shader-dsl-std-lib", "joml").any {
            jar.name.contains(it) && jar.name.endsWith(".jar")
        } }
        .joinToString(" ") { "\"-Xplugin=${it.path}\"" }

    //TODO unix/platform agnostic version
    val args = arrayOf(
        "-Xcontext-receivers",
        "src/main/kotlin/shader.kt",
        "-d", "build/classes/shader-dsl",
        "-classpath", "\"$classpath\"",
        pluginClasspath,
        "-P", "\"plugin:injShaderDslPlugin:resourceOutputDir=build/generated/shader-dsl/main/resources/shaders\""
    )

    commandLine("kotlinc.cmd", *args)
}
