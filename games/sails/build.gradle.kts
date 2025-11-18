plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "io.github.etieskrill.games.sails"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    testImplementation(kotlin("test"))
}

application {
    mainClass = "io.github.etieskrill.games.sails.SailsKt"
}
