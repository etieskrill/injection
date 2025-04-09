plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.20" //TODO VERSION CATALÖÖÖÖÖÖÖG

//    id("com.google.devtools.ksp") version "2.1.20-1.0.32"

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
            implementationClass = "io.github.etieskrill.injection.extension.shader.dsl.gradle.ShaderDslGradlePlugin"
        }
    }
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")
    implementation("io.github.etieskrill.injection.extension.shader.dsl:shader-dsl-lib:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.20")

    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

//    compileOnly("com.google.auto.service:auto-service:1.1.1")
//    ksp("com.google.auto.service:auto-service:1.1.1")

    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
}

tasks.build {
    dependsOn("publishToMavenLocal") //FIXME perhaps not the best of ideas when compiler extension and plugin are in same module
}

tasks.test {
    useJUnitPlatform()
}

publishing { //FIXME probs not even needed as this is an included build
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
