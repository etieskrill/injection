plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())

    implementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
