plugins {
    kotlin("multiplatform")
}

group = "net.bifreus.games.scene.gallery"
version = "0.1.0"

kotlin {
    jvm {
        binaries {
            executable {
                mainClass = "net.bifreus.games.scene.gallery.SceneGalleryKt"
            }
        }
        mainRun {
            mainClass = "net.bifreus.games.scene.gallery.SceneGalleryKt"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine"))
        }
    }
}
