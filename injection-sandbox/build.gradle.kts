import org.jetbrains.kotlin.com.intellij.util.system.OS

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
    implementation(libs.lwjgl.tinyfd)

    val lwjglNatives = when (OS.CURRENT) {
        OS.Windows -> "natives-windows"
        OS.Linux -> "natives-linux"
        else -> error("Unsupported OS: ${OS.CURRENT}")
    }
    implementation("org.lwjgl:lwjgl-tinyfd::$lwjglNatives")
    //natives not specified as they are brought in by the engine

    implementation("io.github.etieskrill.injection.extension.shader.dsl:shader-dsl-plugin:1.0.0-SNAPSHOT")

    implementation(libs.kotlin.coroutines)

    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.2.0")

    implementation("com.github.wendykierp:JTransforms:3.1")
}

application {
    mainClass = "LSystemsKt"
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
