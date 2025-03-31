plugins {
    kotlin("jvm")
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "org.etieskrill.games.ip-demos"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    implementation("org.lwjgl:lwjgl-opengl:3.3.3")
}
