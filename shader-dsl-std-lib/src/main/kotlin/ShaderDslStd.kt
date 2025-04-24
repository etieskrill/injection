package io.github.etieskrill.injection.extension.shader.dsl.std

import io.github.etieskrill.injection.extension.shader.vec3
import org.intellij.lang.annotations.Pattern
import org.joml.Vector3f
import kotlin.reflect.KFunction

annotation class ConstEval

@ConstEval
fun rgb2vec3(@Pattern("[a-zA-Z0-9]{6}") colour: String): vec3 {
    val r = 16 * colour[0].digitToInt(16) + colour[1].digitToInt(16)
    val g = 16 * colour[2].digitToInt(16) + colour[3].digitToInt(16)
    val b = 16 * colour[4].digitToInt(16) + colour[5].digitToInt(16)
    return Vector3f(r.toFloat(), g.toFloat(), b.toFloat())
}

val methods = listOf<KFunction<*>>(::rgb2vec3) //TODO probs move to class instead
