package org.etieskrill.engine.input

fun interface TextInputHandler {

    fun invokeText(character: Char): Boolean

}