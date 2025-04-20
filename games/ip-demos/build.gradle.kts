plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    id("io.github.etieskrill.injection.shader.dsl")
}

group = "org.etieskrill.games.ip-demos"
version = "unspecified"

dependencies {
    implementation(project(":engine"))

    implementation(platform("org.lwjgl:lwjgl-bom:${libs.versions.lwjgl.get()}")) //TODO figure out platforms & catalogs
    implementation(libs.lwjgl.openal)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.stb)

    //natives not specified as they are brought in by the engine
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

application {
    mainClass = "TextureWrappingKt"
}
