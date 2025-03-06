plugins {
    `java-library`
    kotlin("jvm")
}

group = "io.github.etieskrill.extension.shader.dsl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")

    implementation("net.bytebuddy:byte-buddy:1.17.2")

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
