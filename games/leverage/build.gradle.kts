plugins {
    application
    alias(libs.plugins.kotlin.jvm)
//    id("io.github.etieskrill.injection.shader.dsl")
}

group = "io.github.etieskrill.games.leverage"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    implementation("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}")
}

application {
    mainClass = "io.github.etieskrill.games.leverage.AppKt"
}
