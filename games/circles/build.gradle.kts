plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    id("io.github.etieskrill.injection.shader.dsl")
    kotlin("plugin.serialization") version libs.versions.kotlin
}

group = "io.github.etieskrill.games.circles"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation(kotlin("test"))
}

application {
    mainClass = "io.github.etieskrill.games.circles.MainKt"
}
