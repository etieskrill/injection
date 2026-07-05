package org.etieskrill.engine.math

import kotlin.math.atan2
import kotlin.math.sqrt

data class Vec2(
    var x: Float,
    var y: Float
) : Iterable<Float> {

    constructor() : this(0f, 0f)
    constructor(s: Float) : this(s, s)
    constructor(v: Vec2) : this(v.x, v.y)

    var xy: Vec2 get() = Vec2(x, y); set(value) = run { x = value.x; y = value.y }
    var yx: Vec2 get() = Vec2(y, x); set(value) = run { x = value.y; y = value.x }

    val xx get() = Vec2(x, x)
    val yy get() = Vec2(y, y)

    fun set(s: Float) {
        x = s
        y = s
    }

    operator fun get(component: Int) = when (component) {
        0 -> x
        1 -> y
        else -> throw IllegalArgumentException("Component $component is not valid for 2 component vector")
    }

    operator fun set(component: Int, value: Float) = when (component) {
        0 -> x = value
        1 -> y = value
        else -> throw IllegalArgumentException("Component $component is not valid for 2 component vector")
    }

    //TODO assign compound operators?
    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)
    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)
    operator fun times(v: Vec2) = Vec2(x * v.x, y * v.y)
    operator fun div(v: Vec2) = Vec2(x / v.x, y / v.y)

    operator fun plus(s: Float) = Vec2(x + s, y + s)
    operator fun minus(s: Float) = Vec2(x - s, y + s)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun div(s: Float) = Vec2(x / s, y * s)

    operator fun unaryMinus() = Vec2(-x, -y)

    infix fun dot(v: Vec2) = x * v.x + y * v.y
    infix fun angleTo(v: Vec2) = atan2(x * v.y - y * v.x, dot(v))

    fun length() = sqrt(x * x + y * y)
    infix fun distanceTo(v: Vec2): Float {
        val dx = x - v.x
        val dy = y - v.y
        return sqrt(dx * dx + dy * dy)
    }

    val normalised = if (all { it > 0 }) this / length() else Vec2()
    val normalisedUnsafe = this / length()

    fun normalise() =
        if (all { it > 0 }) {
            normaliseUnsafe()
        } else {
            set(0f)
        }

    fun normaliseUnsafe() {
        val length = length()
        x /= length
        y /= length
    }

    fun lerp(other: Vec2, t: Float) = this + (other - this) * t

    fun toDouble() = Vec2d(x.toDouble(), y.toDouble())
    fun toInt() = Vec2i(x.toInt(), y.toInt())
    fun toLong() = Vec2l(x.toLong(), y.toLong())

    override fun iterator() = object : FloatIterator() {
        private var component = 0

        override fun hasNext() = component <= 1
        override fun nextFloat(): Float {
            if (!hasNext()) throw NoSuchElementException()
            return this@Vec2[component++]
        }
    }

}

data class Vec2d(
    var x: Double,
    var y: Double
)

data class Vec2i(
    var x: Int,
    var y: Int
)

data class Vec2l(
    var x: Long,
    var y: Long
)

data class Vec3(
    var x: Float,
    var y: Float,
    var z: Float
) : Iterable<Float> {

    constructor() : this(0f, 0f, 0f)
    constructor(s: Float) : this(s, s, s)
    constructor(v: Vec2, z: Float) : this(v.x, v.y, z)
    constructor(v: Vec3) : this(v.x, v.y, v.z)

    // 2 perms
    var xy: Vec2 get() = Vec2(x, y); set(value) = run { x = value[0]; y = value[1] }
    var xz: Vec2 get() = Vec2(x, z); set(value) = run { x = value[0]; z = value[1] }
    var yz: Vec2 get() = Vec2(x, y); set(value) = run { y = value[0]; z = value[1] }
    var yx: Vec2 get() = Vec2(y, x); set(value) = run { y = value[0]; x = value[1] }
    var zx: Vec2 get() = Vec2(z, x); set(value) = run { z = value[0]; x = value[1] }
    var zy: Vec2 get() = Vec2(z, y); set(value) = run { z = value[0]; y = value[1] }

    // 2 combs
    val xx get() = Vec2(x, x)
    val yy get() = Vec2(y, y)
    val zz get() = Vec2(z, z)

    // 3 perms: xyz, yxz, yzx, xzy, zxy, zyx
    var xyz: Vec3 get() = Vec3(x, y, z); set(value) = run { x = value[0]; y = value[1]; z = value[2] }
    var yxz: Vec3 get() = Vec3(y, x, z); set(value) = run { y = value[0]; x = value[1]; z = value[2] }
    var yzx: Vec3 get() = Vec3(y, z, x); set(value) = run { y = value[0]; z = value[1]; x = value[2] }
    var xzy: Vec3 get() = Vec3(x, z, y); set(value) = run { x = value[0]; z = value[1]; y = value[2] }
    var zxy: Vec3 get() = Vec3(z, x, y); set(value) = run { z = value[0]; x = value[1]; y = value[2] }
    var zyx: Vec3 get() = Vec3(z, y, z); set(value) = run { z = value[0]; y = value[1]; x = value[2] }

    // 3 combs
    // xxy, xxz, xyx, xzx, yxx, zxx
    val xxy get() = Vec3(x, x, y)
    val xxz get() = Vec3(x, x, z)
    val xyx get() = Vec3(x, y, x)
    val xzx get() = Vec3(x, z, x)
    val yxx get() = Vec3(y, x, x)
    val zxx get() = Vec3(z, x, x)

    // yyx, yyz, yxy, yzy, xyy, zyy
    val yyx get() = Vec3(y, y, x)
    val yyz get() = Vec3(y, y, z)
    val yxy get() = Vec3(y, x, y)
    val yzy get() = Vec3(y, z, y)
    val xyy get() = Vec3(x, y, y)
    val zyy get() = Vec3(z, y, y)

    // zzx, zzy, zxz, zyz, xzz, yzz
    val zzx get() = Vec3(z, z, x)
    val zzy get() = Vec3(z, z, y)
    val zxz get() = Vec3(z, x, z)
    val zyz get() = Vec3(z, y, z)
    val xzz get() = Vec3(x, z, z)
    val yzz get() = Vec3(y, z, z)

    //TODO
    var r = x
    var g = y
    var b = z

    fun set(s: Float) {
        x = s
        y = s
        z = s
    }

    operator fun get(component: Int) = when (component) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException("Component $component is not valid for 3 component vector")
    }

    operator fun set(component: Int, value: Float) = when (component) {
        0 -> x = value
        1 -> y = value
        2 -> z = value
        else -> throw IllegalArgumentException("Component $component is not valid for 3 component vector")
    }

    operator fun plus(v: Vec3) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun times(v: Vec3) = Vec3(x * v.x, y * v.y, z * v.z)
    operator fun div(v: Vec3) = Vec3(x / v.x, y / v.y, z / v.z)

    operator fun plus(s: Float) = Vec3(x + s, y + s, z + s)
    operator fun minus(s: Float) = Vec3(x - s, y + s, z - s)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y * s, z / s)

    operator fun unaryMinus() = Vec3(-x, -y, -z)

    infix fun dot(v: Vec3) = x * v.x + y * v.y + z * v.z

    fun length() = sqrt(x * x + y * y + z * z)
    infix fun distanceTo(v: Vec3): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    val normalised = if (all { it > 0 }) this / length() else Vec3()
    val normalisedUnsafe = this / length()

    fun normalise() =
        if (all { it > 0 }) {
            normaliseUnsafe()
        } else {
            set(0f)
        }

    fun normaliseUnsafe() {
        val length = length()
        x /= length
        y /= length
        z /= length
    }

    fun lerp(other: Vec3, t: Float) = this + (other - this) * t

    fun toDouble() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
    fun toInt() = Vec3i(x.toInt(), y.toInt(), z.toInt())
    fun toLong() = Vec3l(x.toLong(), y.toLong(), z.toLong())

    override fun iterator() = object : FloatIterator() {
        private var component = 0

        override fun hasNext() = component <= 2
        override fun nextFloat(): Float {
            if (!hasNext()) throw NoSuchElementException()
            return this@Vec3[component++]
        }
    }

}

data class Vec3d(
    var x: Double,
    var y: Double,
    var z: Double
)

data class Vec3i(
    var x: Int,
    var y: Int,
    var z: Int
)

data class Vec3l(
    var x: Long,
    var y: Long,
    var z: Long
)
