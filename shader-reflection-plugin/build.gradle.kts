plugins {
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0"
}

group = "io.github.etieskrill.injection.extension.shaderreflection"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("ShaderReflectionPlugin") {
            id = "io.github.etieskrill.injection.shaderreflection"
            implementationClass = "io.github.etieskrill.injection.extension.shaderreflection.ShaderReflectionPlugin"
        }
    }
}

val kspVersion: String by project
val jomlVersion: String by project
val jomlPrimitivesVersion: String by project

dependencies {
    implementation("io.github.etieskrill.injection.extension.shaderreflection:shader-interface:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:$jomlVersion")
    implementation("org.joml:joml-primitives:$jomlPrimitivesVersion")

    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    testImplementation("io.mockk:mockk:1.13.16")
}

tasks.test { useJUnitPlatform() }
