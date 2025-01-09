package io.etieskrill.injection.extension.shaderreflection

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ReflectShader(val files: Array<String> = [])
