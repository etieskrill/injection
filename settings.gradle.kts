rootProject.name = "injection"

includeBuild("shader-interface")

includeBuild("shader-reflection-plugin")
includeBuild("shader-reflection-lib")

includeBuild("shader-dsl-plugin")
includeBuild("shader-dsl-lib")
includeBuild("shader-dsl-std-lib")

include("engine")

file("games").listFiles().forEach {
    include("games:${it.name}")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
//    versionCatalogs { //FIXME this should apparently make the catalog available to included builds, but the error is literally not true, unless only one catalog may be built from any given catalog file, which would be stoopid.
//        create("libs") {
//            from(files("../gradle/libs.versions.toml"))
//        }
//    }
}
