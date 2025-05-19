plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.20"
}

group = "io.github.etieskrill.injection.extension.shader.reflection"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("ShaderReflectionPlugin") {
            id = "io.github.etieskrill.injection.shader.reflection"
            implementationClass = "io.github.etieskrill.injection.extension.shader.reflection.ShaderReflectionPlugin"
        }
    }
}

kotlin {
    jvmToolchain(23)
}

val kspVersion: String by project

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
    implementation("io.github.etieskrill.injection.extension.shader.reflection:shader-reflection-lib:1.0.0-SNAPSHOT")

    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
    compileOnly("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.20")
    testImplementation("io.mockk:mockk:1.13.16")
}

tasks.test { useJUnitPlatform() }
