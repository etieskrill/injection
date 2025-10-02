package io.github.etieskrill.sandbox

import java.util.regex.Pattern

fun main() {
    val pattern = Pattern.compile("""^plugin:([^:]*):([^=]*)=(.*)$""")
    val matcher = pattern.matcher("plugin:injShaderDslPlugin:resourceOutputDir=build")
    require(matcher.matches())

    println("Hello World!")
}