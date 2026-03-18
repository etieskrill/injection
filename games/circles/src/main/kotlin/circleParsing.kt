package io.github.etieskrill.games.circles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface CircleSlot

@Serializable
data class PrimaryRune(val id: String) : CircleSlot
@Serializable
data class AuxiliaryRune(val id: String)

@Serializable
data class Circle(
    val focalRune: PrimaryRune?,
    val runes: List<CircleSlot?>,
    val numRings: Int,
    val auxRunes: List<AuxiliaryRune>
) : CircleSlot

//TODO:
// - focal rune definitions, id, name, vis type and amounts, conversions etc.
// - aux rune definitions, id, name, stat, strength

fun main() {
    val primRuneEmpty = PrimaryRune("empty")

    val auxRuneGebo = AuxiliaryRune("gebo")
    val auxRuneNauthiz = AuxiliaryRune("nauthiz")
    val heatingInscription = Circle(
        PrimaryRune("fire"), listOf(),
        1, listOf(auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz)
    )
    println(Json.encodeToString(heatingInscription))

    val multiRune = Circle(
        null, listOf(primRuneEmpty, primRuneEmpty, primRuneEmpty),
        1, listOf()
    )
    println(Json.encodeToString(multiRune))

    val focalRuneCircle = Circle(
        primRuneEmpty, listOf(primRuneEmpty, primRuneEmpty, primRuneEmpty, primRuneEmpty),
        1, listOf()
    )
    println(Json.encodeToString(focalRuneCircle))

    val multiAuxLayers = Circle(
        primRuneEmpty, listOf(),
        2, listOf()
    )
    println(Json.encodeToString(multiAuxLayers))

    val subCircles = Circle(
        null, listOf(
            Circle(null, listOf(primRuneEmpty, primRuneEmpty), 1, listOf()),
            Circle(null, listOf(primRuneEmpty, primRuneEmpty), 1, listOf())
        ), 1, listOf()
    )
    println(Json.encodeToString(subCircles))
}
