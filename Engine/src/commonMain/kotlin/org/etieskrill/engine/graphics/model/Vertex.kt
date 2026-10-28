package org.etieskrill.engine.graphics.model

import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4c
import net.bifreus.kml.Vec4ic
import org.etieskrill.engine.graphics.buffer.VertexArrayAccessor
import kotlin.math.pow
import kotlin.math.roundToLong

data class Vertex(
    val position: Vec3c,
    val normal: Vec3c? = null,
    val textureCoords: Vec2c? = null,
    val tangent: Vec3c? = null,
    val biTangent: Vec3c? = null,
    val bones: Vec4ic? = null,
    val boneWeights: Vec4c? = null,
) {

    companion object {
        object Accessor : VertexArrayAccessor<Vertex>() {
            override fun registerFields() {
                addField<Vec3c> { it, buffer -> buffer += it.position }
                addField<Vec3c> { it, buffer -> buffer += it.normal }
                addField<Vec2c> { it, buffer -> buffer += it.textureCoords }
                addField<Vec3c> { it, buffer -> buffer += it.tangent }
                addField<Vec3c> { it, buffer -> buffer += it.biTangent }
                addField<Vec4ic> { it, buffer -> buffer += it.bones }
                addField<Vec4c> { it, buffer -> buffer += it.boneWeights }
            }
        }
    }

    override fun toString() =
        """Vertex{
            |position=$position, 
            |normal=$normal, 
            |textureCoords=textureCoords, 
            |bones=(${bones?.x}, ${bones?.y}, ${bones?.z}, ${bones?.w}), 
            |boneWeights=(${boneWeights?.x?.round(3)}, ${boneWeights?.y?.round(3)}, 
            |${boneWeights?.z?.round(3)}, ${boneWeights?.w?.round(3)})}
        """.trimMargin()

}

private fun Float.round(decimals: Int): Float {
    val decimalPower = 10f.pow(decimals)
    return (this * decimalPower).roundToLong() / decimalPower
}
