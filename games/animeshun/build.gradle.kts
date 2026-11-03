plugins {
    kotlin("jvm")
    application
}

group = "org.etieskrill.game.animeshun"
version = "1.0.0-SNAPSHOT"

dependencies {
    implementation(project(":engine"))

    implementation(libs.lwjgl.opengl)
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-windows")
}

application {
    mainClass = "org.etieskrill.game.animeshun.GameKt"
}
