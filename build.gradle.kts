plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    //FIXME are these even applied really? - certainly not anymore :P
//    java.toolchain.languageVersion = JavaLanguageVersion.of(javaLanguageVersion)
//    kotlin.jvmToolchain(Integer.parseInt(javaLanguageVersion))
}
