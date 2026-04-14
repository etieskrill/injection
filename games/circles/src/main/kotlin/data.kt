package io.github.etieskrill.games.circles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.etieskrill.engine.entity.Entity
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4fc

typealias GameObject = Entity

@Serializable
data class JsonVisType(val name: String, val colour: Vector4fc)

@Serializable
sealed interface JsonCircleSlot {
    val visCapacity: Float
}

@Serializable
enum class JsonRuneType { RECEPTACLE, STREAM, EMITTER }

@Serializable
data class JsonPrimaryRune(
    val id: String,
    val type: JsonRuneType,
    override val visCapacity: Float,
    val consumption: Map<JsonVisType, Float>
) : JsonCircleSlot

@Serializable
data class JsonAuxiliaryRune(val id: String)

@Serializable
data class JsonCircle(
    override val visCapacity: Float,
    val focalRune: JsonCircleSlot?,
    val runes: List<JsonCircleSlot?>,
    val auxRunes: List<List<List<JsonAuxiliaryRune>>>, // rings -> prim runes -> aux runes
) : JsonCircleSlot

//TODO:
// - focal rune definitions, id, name, vis type and amounts, conversions etc.
// - aux rune definitions, id, name, stat, strength

data class VisType(val name: String, val colour: Vector4fc)

sealed interface CircleSlot {
    val visCapacity: Float
    val storedVis: MutableMap<VisType, Float>

    fun clearVis() = storedVis.clear()
}

sealed interface ReceptacleSlot : CircleSlot

sealed interface StreamSlot : ReceptacleSlot {
    /**
     * The minimum fixed amount of vis that is consumed from the environment per second in order to uphold the vis stream.
     */
    val streamUpkeepAbsolute: Float

    /**
     * The fraction of vis that is consumed from the incoming vis on top of [streamUpkeepAbsolute] per second in order
     * to uphold the vis stream.
     */
    val streamUpkeepRelative: Float
}

open class PrimaryRune(
    open val id: String,
    override val visCapacity: Float,
    override val storedVis: MutableMap<VisType, Float> = mutableMapOf(),
) : CircleSlot {
    override fun toString(): String {
        return "PrimaryRune(id='$id', visCapacity=$visCapacity, storedVis=$storedVis)"
    }
}

data class ReceptacleRune(
    override val id: String,
    override val visCapacity: Float,
) : PrimaryRune(id, visCapacity), ReceptacleSlot

data class StreamRune(
    override val id: String,
    override val visCapacity: Float,
    override val streamUpkeepAbsolute: Float,
    override val streamUpkeepRelative: Float
) : PrimaryRune(id, visCapacity), StreamSlot

data class EmitterRune(
    override val id: String,
    override val visCapacity: Float,
    val consumption: Map<VisType, Float>, // vis/s
    override val storedVis: MutableMap<VisType, Float> = mutableMapOf(),
    val effect: (position: Vector3fc, direction: Vector3fc, placedOn: GameObject) -> Unit
) : PrimaryRune(id, visCapacity)

data class AuxiliaryRune(val id: String)

data class Circle(
    val placedOn: GameObject,
    override val visCapacity: Float,
    override val streamUpkeepAbsolute: Float,
    override val streamUpkeepRelative: Float,
    val focalRune: CircleSlot?,
    val runes: List<CircleSlot?>, // nullable to represent empty slots
    val numRings: Int,
    val auxRunes: List<List<List<AuxiliaryRune>>>, // rings -> prim runes -> aux runes
    override val storedVis: MutableMap<VisType, Float> = mutableMapOf()
) : StreamSlot {
    override fun clearVis() {
        super.clearVis()
        focalRune?.clearVis()
        runes.forEach { it?.clearVis() }
    }
}

fun main() {
    val primRuneEmpty = PrimaryRune("empty", 0f)

    val auxRuneGebo = AuxiliaryRune("gebo")
    val auxRuneNauthiz = AuxiliaryRune("nauthiz")
    val heatingInscription = Circle(
        Entity(0), 100f, 1f, 0.2f,
        PrimaryRune("fire", 10f), listOf(),
        1, listOf(listOf(listOf(auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz)))
    )
    println(Json.encodeToString(heatingInscription))

    val multiRune = Circle(
        Entity(0), 100f, 1f, 0.2f,
        null, listOf(primRuneEmpty, primRuneEmpty, primRuneEmpty), 1, listOf()
    )
    println(Json.encodeToString(multiRune))

    val focalRuneCircle = Circle(
        Entity(0), 100f, 1f, 0.2f,
        primRuneEmpty, listOf(primRuneEmpty, primRuneEmpty, primRuneEmpty, primRuneEmpty), 1, listOf()
    )
    println(Json.encodeToString(focalRuneCircle))

    val multiAuxLayers = Circle(
        Entity(0), 100f, 1f, 0.2f,
        primRuneEmpty, listOf(), 2, listOf()
    )
    println(Json.encodeToString(multiAuxLayers))

    val subCircles = Circle(
        Entity(0), 100f, 1f, 0.2f,
        null, listOf(
            Circle(
                Entity(0), 100f, 1f, 0.2f,
                null, listOf(primRuneEmpty, primRuneEmpty), 1, listOf()
            ),
            Circle(
                Entity(0), 100f, 1f, 0.2f,
                null, listOf(primRuneEmpty, primRuneEmpty), 1, listOf()
            )
        ), 1, listOf()
    )
    println(Json.encodeToString(subCircles))
}
