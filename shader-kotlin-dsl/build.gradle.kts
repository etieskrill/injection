plugins {
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0" //TODO find out why tf this does not work without a version and why the fucking logs do not tell you shit about this

//    id("com.google.devtools.ksp") version "2.1.0-1.0.29"

    `maven-publish`
}

group = "io.github.etieskrill.injection.extension.shader.dsl"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("ShaderDslGradlePlugin") {
            id = "io.github.etieskrill.injection.shader.dsl"
            implementationClass = "io.github.etieskrill.injection.extension.shader.dsl.ShaderDslGradlePlugin"
        }
    }
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")

    implementation("net.bytebuddy:byte-buddy:1.17.2")

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.10")

//    compileOnly("com.google.auto.service:auto-service:1.1.1")
//    ksp("com.google.auto.service:auto-service:1.1.1")

    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
}

tasks.build {
    dependsOn("publishToMavenLocal") //FIXME perhaps not the best of ideas when compiler extension and plugin are in same module
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])

            pom {
                name.set("io.github.etieskrill.injection.autism.maven-publication")
            }
        }
    }

    repositories {
        maven {
//            setUrl(property("sonatypeReleaseUrl")!!)
        }
    }
}
