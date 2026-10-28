plugins {
    kotlin("multiplatform") version "2.1.20"
    `maven-publish`
}

group = "io.github.etieskrill.injection.extension.shader.dsl"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
            implementation("net.bifreus.kml:kml:1.0.0")
            runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.2.20")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.etieskrill.injection.extension.shader.dsl"
            artifactId = "shader-dsl-lib"
            version = "1.0.0-SNAPSHOT"

            from(components["kotlin"])
        }
    }
}
