plugins {
    kotlin("multiplatform") version "2.1.20"
    `maven-publish`
}

repositories { //TODO FINALLY do convention plugins, also FIXME in the settings.gradle included builds like this one are apparently not subject to stuff like allprojects etc.
    mavenLocal()
    mavenCentral()
}

group = "io.github.etieskrill.injection.extension.shader"
version = "1.0.0-SNAPSHOT"

kotlin {
//    jvmToolchain(23) //TODO in "jvm" clause below?

    jvm()

    sourceSets {
        commonMain.dependencies {
            api("net.bifreus.kml:kml:1.0.0")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.etieskrill.injection.extension.shader"
            artifactId = "shader-interface"
            version = "1.0.0-SNAPSHOT"

            from(components["kotlin"])
        }
    }
}
