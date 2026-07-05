package io.github.etieskrill.injection.extension.shader.reflection

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ReflectShader(val files: Array<String> = [])
