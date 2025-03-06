plugins {
    `java-library`
    kotlin("jvm") version "2.1.0"
}

repositories { //TODO FINALLY do convention plugins
    mavenCentral()
}

group = "io.github.etieskrill.injection.extension.shader"
version = "1.0.0-SNAPSHOT"

dependencies {
    implementation("org.joml:joml:1.10.8")
}
