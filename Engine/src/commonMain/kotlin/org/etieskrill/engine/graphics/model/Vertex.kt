package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.joml.Vector4ic
import kotlin.math.pow
import kotlin.math.roundToLong

data class Vertex(
    val position: Vector3fc,
    val normal: Vector3fc? = null,
    val textureCoords: Vector2fc? = null,
    val tangent: Vector3fc? = null,
    val biTangent: Vector3fc? = null,
    val bones: Vector4ic? = null,
    val boneWeights: Vector4fc? = null,
) {

    companion object {
        object Accessor : VertexArrayAccessor<Vertex>() {
            override fun registerFields() {
                addField<Vector3fc> { it, buffer -> buffer += it.position }
                addField<Vector3fc> { it, buffer -> buffer += it.normal }
                addField<Vector2fc> { it, buffer -> buffer += it.textureCoords }
                addField<Vector3fc> { it, buffer -> buffer += it.tangent }
                addField<Vector3fc> { it, buffer -> buffer += it.biTangent }
                addField<Vector4ic> { it, buffer -> buffer += it.bones }
                addField<Vector4fc> { it, buffer -> buffer += it.boneWeights }
            }
        }
    }

    override fun toString() =
        """Vertex{
            |position=$position, 
            |normal=$normal, 
            |textureCoords=textureCoords, 
            |bones=(${bones?.x()}, ${bones?.y()}, ${bones?.z()}, ${bones?.w()}), 
            |boneWeights=(${boneWeights?.x()?.round(3)}, ${boneWeights?.y()?.round(3)}, 
            |${boneWeights?.z()?.round(3)}, ${boneWeights?.w()?.round(3)})}
        """.trimMargin()

}

private fun Float.round(decimals: Int): Float {
    val decimalPower = 10f.pow(decimals)
    return (this * decimalPower).roundToLong() / decimalPower
}
