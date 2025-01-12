package io.github.etieskrill.injection.extension.shaderreflection

interface AbstractShader {
    fun setUniform(name: String, value: Any)
    fun setUniformArray(name: String, value: Array<Any>)
    fun setUniformArray(name: String, index: Int, value: Any)
}
