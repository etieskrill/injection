plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"

    `maven-publish`
}

group = "io.github.etieskrill.injection.extension.shader.dsl.std"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
    implementation("io.github.etieskrill.injection.extension.shader.dsl:shader-dsl-lib:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.etieskrill.injection.extension.shader.dsl.std"
            artifactId = "shader-dsl-std-lib"
            version = "1.0.0-SNAPSHOT"

            from(components["kotlin"])
        }
    }
}
