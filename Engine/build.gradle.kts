@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.com.intellij.util.system.OS
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    `maven-publish`

    id("io.github.etieskrill.injection.shader.reflection")
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "org.etieskrill.engine"
version = "1.0.0-SNAPSHOT"

kotlin {
    compilerOptions.freeCompilerArgs.addAll(
        "-Xcontext-receivers",
        "-Xexpect-actual-classes"
    )

    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(libs.kotlin.stdlib)
            implementation(libs.kotlin.coroutines)

            //LWJGL native interfaces
            implementation(platform("org.lwjgl:lwjgl-bom:${libs.versions.lwjgl.get()}"))

            implementation(libs.bundles.lwjgl)

            val arch = if (System.getProperty("os.arch").contains("aarch64")) "-arm64" else ""
            val lwjglNatives = when (OS.CURRENT) {
                OS.Windows -> "natives-windows$arch"
                OS.Linux -> "natives-linux$arch"
                else -> error("Unsupported OS: ${OS.CURRENT}")
            }

            //TODO surely these can be moved to the version catalog too
            runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-freetype::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
            runtimeOnly("org.lwjgl:lwjgl-meshoptimizer::$lwjglNatives")

            api("io.github.etieskrill.injection.extension.shader.dsl:shader-dsl-lib:1.0.0-SNAPSHOT")

            //OpenGL-oriented maths library
            api(libs.joml)
            api(libs.joml.primitives)

            implementation(libs.jetbrains.annotations)

            implementation(libs.snakeyaml)

            //Logging
            api(libs.slf4j.api) //FIXME compileOnlyApi? >:)
            runtimeOnly(libs.slf4j.simple)
//            implementation "org.slf4j:slf4j-reload4j:2.0.17" //FIXME something about the build config is **REALLY** wrong
            api("io.github.oshai:kotlin-logging-jvm:7.0.13")

            //Test framework and libraries
//            testImplementation(libs.junit.jupiter)
//            testImplementation(libs.hamcrest)
//            testImplementation(libs.mockito.junit.jupiter)
//            testImplementation(libs.kotlin.test)
        }
    }
}

//tasks.test {
//    useJUnitPlatform()
//}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.etieskrill.engine"
            artifactId = "engine"
            version = project.version.toString()

            from(components["kotlin"])
        }
    }
}
