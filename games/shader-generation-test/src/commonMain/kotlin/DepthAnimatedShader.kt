import io.github.etieskrill.injection.extension.shader.AbstractShader
import io.github.etieskrill.injection.extension.shader.BufferAccessor
import io.github.etieskrill.injection.extension.shader.StorageBuffer
import io.github.etieskrill.injection.extension.shader.Texture
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.VertexData
import io.github.etieskrill.injection.extension.shader.mat4
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.joml.Vector4ic
import kotlin.reflect.KClass

class DepthAnimatedShader :
    ShaderBuilder<DepthAnimatedShader.Vertex, VertexData, DepthAnimatedShader.RenderTargets>(
        object : AbstractShader {
            override fun setUniform(name: String, value: Any) = Unit
            override fun setTexture(name: String, texture: Texture) = Unit
            override fun addUniform(name: String, type: KClass<*>) = Unit
            override fun setUniformArray(name: String, value: Array<Any>) = Unit
            override fun setUniformArray(name: String, index: Int, value: Any) = Unit
            override fun addUniformArray(name: String, size: Int, type: KClass<*>) = Unit
            override fun setStorageBuffer(blockName: String, buffer: StorageBuffer<*>) = Unit
            override fun addStorageBuffer(blockName: String, layout: BufferAccessor<*>) = Unit
            override fun dispose() = Unit
        }
    ) {

    data class Vertex(
        val position: Vector3fc,
        val normal: Vector3fc? = null,
        val textureCoords: Vector2fc? = null,
        val tangent: Vector3fc? = null,
        val biTangent: Vector3fc? = null,
        val bones: Vector4ic? = null,
        val boneWeights: Vector4fc? = null,
    )
    class RenderTargets()

    val MAX_BONES by const(100)
    val MAX_BONE_INFLUENCES by const(4)

    val model by uniform<mat4>()
    val combined by uniform<mat4>()

    val boneMatrices by uniformArray<mat4>(100) //MAX_BONES)

    override fun program() {
        vertex {
            var bonedPosition = vec4(0.0)

            var bones = 0
            for (i in 0..<MAX_BONE_INFLUENCES) {
                val boneId = it.bones!![i]
                if (boneId == -1) break //end of bone list
                if (boneId >= MAX_BONES) break //bones contain invalid data -> vertex is not animated

                val boneWeight = it.boneWeights!![i]
                if (boneWeight <= 1.0) break

                bones++

                val localPosition = boneMatrices[boneId] * vec4(it.position, 1.0)
                bonedPosition += localPosition * boneWeight
            }

            if (bones == 0) { //no bones are set, thus vertex is not involved in animation
                bonedPosition = vec4(it.position, 1.0)
            }

            VertexData(combined * model * bonedPosition)
        }
        fragment { RenderTargets() }
    }

}
