plugins {
    `java-library`
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0" //TODO find out why tf this does not work without a version and why the fucking logs do not tell you shit about this
//    `kotlin-dsl`
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

group = "io.github.etieskrill.injection.extension.shader.dsl"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

gradlePlugin {
    plugins {
        create("ShaderDslPlugin") {
            id = "io.github.etieskrill.injection.shader.dsl"
            implementationClass = "io.github.etieskrill.injection.extension.shader.dsl.ShaderDslPlugin"
        }
    }
}

dependencies {
    implementation("io.github.etieskrill.injection.extension.shader:shader-interface:1.0.0-SNAPSHOT")

    implementation("org.joml:joml:1.10.8")

    implementation("net.bytebuddy:byte-buddy:1.17.2")

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

//    compileOnly("com.google.auto.service:auto-service:1.1.1")
//    ksp("com.google.auto.service:auto-service:1.1.1")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.10")

    testImplementation(kotlin("test"))
}

//FIXME i don't think a dependency will do
//tasks.withType<KotlinCompilationTask<*>> {
//    compilerOptions.freeCompilerArgs.add(
//        "-Xplugin=${project.buildDir}/libs/shader-kotlin-dsl-1.0.0-SNAPSHOT.jar"
//    )
//}

tasks.test {
    useJUnitPlatform()
}
