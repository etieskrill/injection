plugins {
    `java-library`
    kotlin("jvm") version "2.1.20" //TODO VERSION CATALÖÖÖÖÖÖÖG
}

group = "io.github.etieskrill.injection.extension.shader.dsl"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.20")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
