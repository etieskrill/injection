package io.github.etieskrill.injection

//FIXME
class ShaderReflectionTest {

    private val primitiveDetectionTestData = mapOf(
        "uniform vec2 vector;" to mapOf("vector" to "vec2"),
        "uniform\nvec2\nvector;" to mapOf("vector" to "vec2"),
        "uniform vec2 vector" to mapOf(),
        "vec2 vector;" to mapOf(),
        "" to mapOf(),
        "uniform vec2 vector;\nuniform mat3 matrix;" to mapOf("vector" to "vec2", "matrix" to "mat3"),
        "uniform vec2 vector\nuniform mat3 matrix;" to mapOf("matrix" to "mat3"),
        "#version 430 core" to mapOf(),
        "in vec4 fragPos;" to mapOf(),
        "out vec4 fragColour;" to mapOf()
    )

//    @TestFactory
//    fun `Test Primitive Uniform Detection`() = primitiveDetectionTestData
//        .map { (input, expected) ->
//            DynamicTest.dynamicTest("The source code $input should yield the uniforms $expected") {
//                assertEquals(getPrimitiveUniforms(expected), input)
//            }
//        }

}