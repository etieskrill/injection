package io.github.etieskrill.extension.shaderreflection

interface AbstractShader {
    fun setUniform(name: String, value: Any)
    fun setUniformArray(name: String, value: Array<*>)
    fun setUniformArray(name: String, index: Int, value: Any)
}
