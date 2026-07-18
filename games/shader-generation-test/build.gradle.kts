plugins {
    kotlin("multiplatform") version "2.1.20"
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "io.github.etieskrill.sandbox"
version = "unspecified"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
}
