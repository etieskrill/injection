package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.joml.Vector4ic
import java.nio.ByteBuffer
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
            //FIXME hwat?
//            override val elementByteSize: Int get() = 88 //TODO brother fields.sumOf { it.fieldByteSize }

            override fun registerFields() {
                addField<Vector3fc> { it, buffer -> it.position.get(buffer) }
                addField<Vector3fc> { it, buffer ->
                    it.normal?.get(buffer) ?: buffer.putFloat(0f).putFloat(0f).putFloat(0f)
                }
                addField<Vector2fc> { it, buffer -> it.textureCoords?.get(buffer) ?: buffer.putFloat(0f).putFloat(0f) }
                addField<Vector3fc> { it, buffer ->
                    it.tangent?.get(buffer) ?: buffer.putFloat(0f).putFloat(0f).putFloat(0f)
                }
                addField<Vector3fc> { it, buffer ->
                    it.biTangent?.get(buffer) ?: buffer.putFloat(0f).putFloat(0f).putFloat(0f)
                }
                addField<Vector4ic> { it, buffer ->
                    it.bones?.get(buffer) ?: buffer.putInt(0).putInt(0).putInt(0).putInt(0)
                }
                addField<Vector4fc> { it, buffer ->
                    it.boneWeights?.get(buffer) ?: buffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f)
                }
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
