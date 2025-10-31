plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"

    `maven-publish`
}

group = "io.github.etieskrill.injection.extension.shader.reflection"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.etieskrill.injection.extension.shader.reflection"
            artifactId = "shader-reflection-lib"
            version = "1.0.0-SNAPSHOT"

            from(components["kotlin"])
        }
    }
}
