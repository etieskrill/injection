plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"
}

repositories { //TODO FINALLY do convention plugins, also FIXME in the settings.gradle included builds like this one are apparently not subject to stuff like allprojects etc.
    mavenLocal()
    mavenCentral()
}

group = "io.github.etieskrill.injection.extension.shader"
version = "1.0.0-SNAPSHOT"

dependencies {
    implementation("org.joml:joml:1.10.8")
}

kotlin { jvmToolchain(23) }

