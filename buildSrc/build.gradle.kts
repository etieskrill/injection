plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val kspVersion = "2.0.21-1.0.28"

dependencies {
    implementation(gradleApi())

//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    implementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
