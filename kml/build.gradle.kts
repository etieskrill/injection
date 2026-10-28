plugins {
    kotlin("multiplatform") version "2.1.20"
}

group = "net.bifreus.kml"
version = "1.0.0"

repositories { //TODO FINALLY do convention plugins, also FIXME in the settings.gradle included builds like this one are apparently not subject to stuff like allprojects etc.
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
}
