package injection.sandbox

import injection.sandbox.math.*

fun vector() {

    val zeroVec = Vec2(0)
    val vec2 = Vec2(1.5, 2.5)
    val vec3 = Vec3(vec2, 1)
    Vec2(vec3)
    vec3.toVec2()
    val ivec = IVec2(1, 2)
    val bvec = BVec2(true, false)

    val position = Vec3(2, 3, 4)
    val newPosition = position + -Vec3(1, 2, 3)
    val componentMultiply = position * newPosition
    val dotProduct = position dot newPosition
    val crossProduct = position cross newPosition

    position + 3
    3 - position
    position * 3
    3 / position

    position == newPosition

    //does it even make sense tho?
//    position > 0 //must be true for all components
//    position <= newPosition //must be true for all components

    position.x
    position.y
    position.z
    //position.w

    val colour = Vec4(1, 0, 1, 1)
    colour.r
    colour.b
    colour.g
    colour.a

    val textureCoords = Vec2(0.5, 0.75)
    textureCoords.u
    textureCoords.v
    //textureCoords.w

    for (component in 0..2) {
        println("Component ${component + 1} is ${position[component]}")
    }
    for ((index, component) in position.withIndex()) {
        println("Component ${index + 1} is $component")
    }

    val (x, y, z) = position

    !bvec

    position.length
    position.normal //SHOULD NEVER BE FUCKING NaN EVEN IF IT DOES NOT MAKE SENSE
    position.normalize()

    position.normalUnsafe
    position.normalizeUnsafe()

    position.x = 0f
    val xy: Vec2 = position.xy
    val (y2, z2) = position.yz
    newPosition.xy = xy
    position.yz = Vec2(3, 4)

    position += newPosition
    position *= Vec3(2)
    position *= 2

    println("Weee")

}

fun matrix() {

    val mat2 = Mat2()
    val mat3 = Mat3(
        0f, 0f, 1f,
        0f, 1f, 0f,
        1f, 0f, 0f
    )

    assert(Mat3().transposed == mat3)

    val position = Vec3(1, 2, 3)
    val newPosition = Mat3().translation(-3, -2, -1) * position

    val mat32 = mat3.transpose() * mat3
    -mat3
    mat3 + mat3

    val vec1 = Vec4(1, 1, 0, 0)
    val vec2 = Vec4(0, 0, 1, 1)

    val mat4 = Mat4(vec1, vec2, vec1, vec2)
    mat4 / 8
    mat4 += 3
    mat4 * mat4

    val m10: Float = mat4[1, 0]
    val m2: Vec4 = mat4[2]

    val m21: Float= mat4.m21

    val mat42 = mat4.translated(1, 2, 3)
    mat4.rotationXY(15.deg)
    mat4.scaling(10)
}

//fun quaternion() {
//}

fun main() {
    vector()
    matrix()
//    quaternion()
}
