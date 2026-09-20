plugins {
    kotlin("multiplatform")
    id("io.github.etieskrill.injection.shader.dsl")
    kotlin("plugin.serialization") version libs.versions.kotlin
}

group = "io.github.etieskrill.games.circles"
version = "unspecified"

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")

    jvm {
        binaries {
            executable {
                mainClass = "io.github.etieskrill.games.circles.MainKt"
            }
        }
        mainRun {
            mainClass = "io.github.etieskrill.games.circles.MainKt"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":engine"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
