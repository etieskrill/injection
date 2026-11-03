@file:Suppress("unused")

package net.bifreus.kml

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The implementations of most functions are just a brazen (and in many ways less performant) copy of the ones in
 * - [JOML](https://github.com/JOML-CI/JOML) for vectors and matrices and
 * - [joml-primitives](https://github.com/JOML-CI/joml-primitives) for geometric primitives
 */

typealias Rotation = Number

const val PI_f = PI.toFloat()
const val PI_OVER_2_f = (PI / 2f).toFloat()

val Rotation.radians get() = toFloat()
val Rotation.rad get() = radians

val Rotation.degrees get() = toFloat() * (PI_f / 180f)
val Rotation.deg get() = degrees

val Rotation.turn get() = toFloat() / (2f * PI_f)
val Rotation.tr get() = turn

val Rotation.tau get() = 2f * toFloat()

fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)
fun ClosedRange<Float>.lerp(t: Float) = start + t * (endInclusive - start)

interface Vec2c : Iterable<Float> {
    val x: Float
    val y: Float

    val xy: Vec2
    val yx: Vec2
    val xx: Vec2
    val yy: Vec2

    val u: Float
    val v: Float

    val uv: Vec2
    val vu: Vec2
    val uu: Vec2
    val vv: Vec2

    val length: Float

    /**
     * Returns a normalised copy of this vector. Returns the zero vector instead of `NaN` if this vector is zero.
     */
    val normal: Vec2

    /**
     * Returns a normalised copy of this vector.
     */
    val normalUnsafe: Vec2

    /**
     * Returns the dot product between [this][Vec2c] and [v].
     */
    infix fun dot(v: Vec2c): Float

    /**
     * Returns the counterclockwise perp (-endicular) of [this][Vec2c].
     */
    val perp: Vec2

    /**
     * Returns the perp (-endicular) product between [this][Vec2c] and [v].
     */
    infix fun perp(v: Vec2c): Float

    /**
     * Returns the angle in radians between [this][Vec2c] and [v].
     */
    infix fun angleTo(v: Vec2c): Float

    /**
     * Returns the distance between the points defined by [this][Vec2c] and [v].
     */
    infix fun distanceTo(v: Vec2c): Float

    /**
     * Linearly interpolates between the vectors [this][Vec2c] and [other] based on 0 <= [t] <= 1.
     */
    fun lerp(other: Vec2c, t: Float): Vec2

    fun min(v: Vec2c): Vec2
    fun max(v: Vec2c): Vec2

    /**
     * Returns the index of the component with the smallest value.
     */
    fun minComponent(): Int

    /**
     * Returns the index of the component with the greatest value.
     */
    fun maxComponent(): Int

    operator fun plus(v: Vec2c) = Vec2(x + v.x, y + v.y)
    operator fun plus(s: Number) = Vec2(x + s.toFloat(), y + s.toFloat())
    operator fun minus(v: Vec2c) = Vec2(x - v.x, y - v.y)
    operator fun minus(s: Number) = Vec2(x - s.toFloat(), y - s.toFloat())
    operator fun unaryMinus() = Vec2(-x, -y)
    operator fun times(v: Vec2c) = Vec2(x * v.x, y * v.y)
    operator fun times(s: Number) = Vec2(x * s.toFloat(), y * s.toFloat())
    operator fun div(v: Vec2c) = Vec2(x / v.x, y / v.y)
    operator fun div(s: Number) = Vec2(x / s.toFloat(), y / s.toFloat())

    operator fun get(i: Int): Float
    operator fun set(i: Int, s: Number)

    fun toInt(): Vec2i

    fun toMutable(): Vec2 = Vec2(x, y)
}

// @formatter:off
data class Vec2(override var x: Float, override var y: Float) : Vec2c {
    override var xy: Vec2 get() = Vec2(this); set(value) { this(value) }
    override var yx: Vec2 get() = Vec2(y, x); set(value) { this(y = value.x, x = value.y) }
    override val xx: Vec2 get() = Vec2(x, x)
    override val yy: Vec2 get() = Vec2(y, y) //this is gonna suck for Vec4

    override var u = x
    override var v = y
    override var uv: Vec2 get() = xy; set(value) = run { xy = value }
    override var vu: Vec2 get() = yx; set(value) = run { yx = value }
    override val uu: Vec2 get() = xx
    override val vv: Vec2 get() = yy

    override val length get() = sqrt(x * x + y * y)

    override val normal get() = length.let { if (it > 0) this / it else Vec2(0) }
    override val normalUnsafe get() = this / length

    constructor() : this(0)
    constructor(s: Number) : this(s, s)
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())
    constructor(v: Vec2c) : this(v.x, v.y)
    constructor(v: Vec3c) : this(v.x, v.y)

    /**
     * Normalises this vector. Sets this to the zero vector instead of `NaN` if this vector was zero.
     */
    fun normalise(): Vec2 {
        val length = length
        if (length == 0f) return apply { this(0f) }
        x /= length
        y /= length
        return this
    }

    /**
     * Normalises this vector.
     */
    fun normaliseUnsafe(): Vec2 {
        val length = length
        x /= length
        y /= length
        return this
    }

    override infix fun dot(v: Vec2c) = x * v.x + y * v.y

    override val perp: Vec2 get() = Vec2(-y, x)
    override fun perp(v: Vec2c) = x * v.y - y * v.x

    override fun angleTo(v: Vec2c) = atan2(x * v.y - y * v.x, dot(v))
    override fun distanceTo(v: Vec2c): Float {
        val dx = x - v.x
        val dy = y - v.y
        return sqrt(dx * dx + dy * dy)
    }

    override fun lerp(other: Vec2c, t: Float) = this + (other - this) * t

    override fun min(v: Vec2c): Vec2 = TODO("Not yet implemented")
    override fun max(v: Vec2c): Vec2 = TODO("Not yet implemented")

    override fun minComponent() = if (x < y) 0 else 1
    override fun maxComponent() = if (x > y) 0 else 1

    override operator fun get(i: Int): Float = when (i) {
        0 -> x; 1 -> y; else -> error("Number $i is not a valid component for Vec2")
    }

    override operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); else -> error("Number $i is not a valid component for Vec2")
    }

    operator fun invoke(v: Vec2c) = this(v.x, v.y)
    operator fun invoke(v: Vec2ic) = this(v.x, v.y)
    operator fun invoke(s: Number) = this(s, s)
    operator fun invoke(x: Number? = null, y: Number? = null): Vec2 {
        x?.let { this.x = it.toFloat() }
        y?.let { this.y = it.toFloat() }
        return this
    }

    override fun equals(other: Any?) = other is Vec2 && x == other.x && y == other.y

    override fun iterator(): Iterator<Float> = object : Iterator<Float> {
        var i = 0
        override fun next() = get(i++)
        override fun hasNext() = i < 2
    }

    override fun toInt() = Vec2i(x.toInt(), y.toInt())

    fun toConst() = Vec2(this) as Vec2c
}

operator fun Number.plus(v: Vec2c) = v + this
operator fun Number.minus(v: Vec2c) = Vec2(this.toFloat() - v.x, this.toFloat() - v.y)
operator fun Number.times(v: Vec2c) = v * this
operator fun Number.div(v: Vec2c) = Vec2(this.toFloat() / v.x, this.toFloat() / v.y)

interface Vec2ic : Iterable<Int> {
    val x: Int
    val y: Int

    fun toFloat() = Vec2(x, y)

    fun toMutable() = Vec2i(x, y)
}

data class Vec2i(override var x: Int, override var y: Int) : Vec2ic {
    constructor() : this(0)
    constructor(s: Int) : this(s, s)
    constructor(v: Vec2ic) : this(v.x, v.y)
    constructor(v: Vec2c) : this(v.x.toInt(), v.y.toInt())
    constructor(v: Vec3c) : this(v.x.toInt(), v.y.toInt())

    companion object {
        val ZERO = Vec2i(0, 0).toConst()
    }

    operator fun invoke(other: Vec2ic) = this(other.x, other.y)
    operator fun invoke(x: Int? = null, y: Int? = null) {
        x?.let { this.x = it }
        y?.let { this.y = it }
    }

    override fun iterator() = TODO("Not yet implemented")

    fun toConst() = Vec2i(x, y) as Vec2ic
}

interface Vec3c : Iterable<Float> {
    val x: Float
    val y: Float
    val z: Float

    val length: Float

    val normal: Vec3
    val normalUnsafe: Vec3

    infix fun dot(v: Vec3c): Float
    infix fun cross(v: Vec3c): Vec3

    infix fun angleTo(v: Vec3c): Float
    infix fun distanceTo(v: Vec3c): Float

    fun lerp(other: Vec3c, t: Float): Vec3

    /**
     * Returns a copy of `this` vector rotated around the y-axis by [y] radians, around the x-axis by [x] radians, and
     * around the z-axis by [z] radians, **in that order**.
     *
     * Use [Quat] for order-independent rotations.
     */
    fun rotate(x: Float? = null, y: Float? = null, z: Float? = null): Vec3

    /**
     * Returns the index of the component with the smallest value.
     */
    fun minComponent(): Int

    /**
     * Returns the index of the component with the greatest value.
     */
    fun maxComponent(): Int

    operator fun plus(v: Vec3c) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun plus(s: Number) = Vec3(x + s.toFloat(), y + s.toFloat(), z + s.toFloat())
    operator fun minus(v: Vec3c) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun minus(s: Number) = Vec3(x - s.toFloat(), y - s.toFloat(), z - s.toFloat())
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    operator fun times(v: Vec3c) = Vec3(x * v.x, y * v.y, z * v.z)
    operator fun times(s: Number) = Vec3(x * s.toFloat(), y * s.toFloat(), z * s.toFloat())
    operator fun div(v: Vec3c) = Vec3(x / v.x, y / v.y, z / v.z)
    operator fun div(s: Number) = Vec3(x / s.toFloat(), y / s.toFloat(), z / s.toFloat())

    operator fun get(i: Int): Float
    operator fun set(i: Int, s: Number)

    fun toInt(): Vec3i

    fun toMutable() = Vec3(x, y, z)

    fun equals(x: Float, y: Float, z: Float) = this.x == x && this.y == y && this.z == z
}

data class Vec3(override var x: Float, override var y: Float, override var z: Float) : Vec3c {
    var xy: Vec2 get() = Vec2(x, y); set(value) { this(x = value.x, y = value.y) }
    var yx: Vec2 get() = Vec2(y, x); set(value) { this(y = value.x, x = value.y) }
    var xz: Vec2 get() = Vec2(x, z); set(value) { this(x = value.x, z = value.y) }
    var zx: Vec2 get() = Vec2(z, x); set(value) { this(z = value.x, x = value.y) }
    var yz: Vec2 get() = Vec2(y, z); set(value) { this(y = value.y, z = value.y) }
    var zy: Vec2 get() = Vec2(z, y); set(value) { this(z = value.y, y = value.y) }
    var xyz: Vec3 get() = Vec3(this); set(value) { this(value) }

    var r = x
    var g = y
    var b = z

    var u = x
    var v = y
    var w = z

    override val length get() = sqrt(x * x + y * y + z * z)
    override val normal get() = length.let { if (it > 0) this / it else Vec3(0) }
    override val normalUnsafe get() = this / length

    constructor() : this(0)
    constructor(s: Number) : this(s, s, s)
    constructor(x: Number, y: Number, z: Number) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(v: Vec2c, z: Number) : this(v.x, v.y, z)
    constructor(v: Vec2ic, z: Number) : this(v.x, v.y, z)
    constructor(v: Vec3c) : this(v.x, v.y, v.z)
    constructor(v: Vec3ic) : this(v.x, v.y, v.z)

    companion object {
        val ZERO = Vec3(0).toConst()
        val UP = Vec3(0, 1, 0).toConst()
        val FORWARD = Vec3(0, 0, 1).toConst()
    }

    fun normalize(): Vec3 {
        val length = length
        if (length == 0f) return apply { this(0f) }
        x /= length
        y /= length
        z /= length
        return this
    }

    fun normalizeUnsafe(): Vec3 {
        val length = length
        x /= length
        y /= length
        z /= length
        return this
    }

    override infix fun dot(v: Vec3c) = x * v.x + y * v.y + z * v.z
    override infix fun cross(v: Vec3c) = Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    override infix fun angleTo(v: Vec3c): Float = TODO("Not yet implemented")
    override infix fun distanceTo(v: Vec3c): Float = TODO("Not yet implemented")

    override fun lerp(other: Vec3c, t: Float): Vec3 = Vec3(
        x + t * (other.x - x), y + t * (other.y - y), z + t * (other.z - z)
    )

    override fun rotate(x: Float?, y: Float?, z: Float?): Vec3 {
        if (x != null) {
            val sin = sin(x)
            val cos = cos(x)
            val y = this.y * cos - this.z * sin
            val z = this.y * sin + this.z * cos
            this.y = y
            this.z = z
        }

        if (y != null) {
            val sin = sin(y)
            val cos = cos(y)
            val x = this.x * cos + this.z * sin
            val z = -this.x * sin + this.z * cos
            this.x = x
            this.z = z
        }

        if (z != null) {
            val sin = sin(z)
            val cos = cos(z)
            val x = this.x * cos - this.y * sin
            val y = this.x * sin + this.y * cos
            this.x = x
            this.y = y
        }

        return this
    }

    override fun minComponent() =
        if (x < y) {
            if (x < z) 0 else 2
        } else {
            if (y < z) 1 else 2
        }

    override fun maxComponent() =
        if (x > y) {
            if (x > z) 0 else 2
        } else {
            if (y > z) 1 else 2
        }
    override operator fun get(i: Int) = when (i) {
        0 -> x; 1 -> y; 2 -> z; else -> error("Number $i is not a valid component for Vec3")
    }

    override operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); 2 -> z = s.toFloat()
        else -> error("Number $i is not a valid component for Vec3")
    }

    operator fun invoke(v: Vec3c) = this(v.x, v.y, v.z)
    operator fun invoke(s: Number) = this(s, s, s)
    operator fun invoke(x: Number? = null, y: Number? = null, z: Number? = null): Vec3 {
        x?.let { this.x = it.toFloat() }
        y?.let { this.y = it.toFloat() }
        z?.let { this.z = it.toFloat() }
        return this
    }

    //TODO include epsilon?
    override fun equals(other: Any?) = other is Vec3 && x == other.x && y == other.y && z == other.z

    override fun iterator(): Iterator<Float> = object : Iterator<Float> {
        var i = 0
        override fun next() = get(i++)
        override fun hasNext() = i < 3
    }

    override fun toInt() = Vec3i(x.toInt(), y.toInt(), z.toInt())

    fun toConst() = Vec3(this) as Vec3c
}

operator fun Number.plus(v: Vec3c) = v + this
operator fun Number.minus(v: Vec3c) = Vec3(this.toFloat() - v.x, this.toFloat() - v.y, this.toFloat() - v.z)
operator fun Number.times(v: Vec3c) = v * this
operator fun Number.div(v: Vec3c) = Vec3(this.toFloat() / v.x, this.toFloat() / v.y, this.toFloat() / v.z)

interface Vec3ic : Iterable<Int> {
    val x: Int
    val y: Int
    val z: Int
}

data class Vec3i(override var x: Int, override var y: Int, override var z: Int) : Vec3ic {
    override fun iterator() = TODO("Not yet implemented")
}

interface Vec4c : Iterable<Float> {
    val x: Float
    val y: Float
    val z: Float
    val w: Float

    val length: Float

    val normal: Vec4
    val normalUnsafe: Vec4

    infix fun dot(v: Vec4c): Float
    infix fun cross(v: Vec4c): Vec4

    fun lerp(other: Vec4c, t: Float): Vec4

    /**
     * Returns the index of the component with the smallest value.
     */
    fun minComponent(): Int

    /**
     * Returns the index of the component with the greatest value.
     */
    fun maxComponent(): Int

    operator fun plus(v: Vec4c) = Vec4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun plus(s: Number) = Vec4(x + s.toFloat(), y + s.toFloat(), z + s.toFloat(), w + s.toFloat())
    operator fun minus(v: Vec4c) = Vec4(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun minus(s: Number) = Vec4(x - s.toFloat(), y - s.toFloat(), z - s.toFloat(), w - s.toFloat())
    operator fun unaryMinus() = Vec4(-x, -y, -z, -w)
    operator fun times(v: Vec4c) = Vec4(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun times(s: Number) = Vec4(x * s.toFloat(), y * s.toFloat(), z * s.toFloat(), w * s.toFloat())
    operator fun div(v: Vec4c) = Vec4(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun div(s: Number) = Vec4(x / s.toFloat(), y / s.toFloat(), z / s.toFloat(), w / s.toFloat())

    operator fun get(i: Int): Float
    operator fun set(i: Int, s: Number)

    fun toInt(): Vec4i

    fun toMutable() = Vec4(this)
}

data class Vec4(override var x: Float, override var y: Float, override var z: Float, override var w: Float) : Vec4c {
    //and these are only the combinations, meaning only 10 out of 36 permutations so far, and like 10 out of 336 (i believe) replacement permutations... sigh
    var xy: Vec2 get() = Vec2(x, y); set(value) { this(x = value.x, y = value.y) }
    var xz: Vec2 get() = Vec2(x, z); set(value) { this(x = value.x, z = value.y) }
    var xw: Vec2 get() = Vec2(x, w); set(value) { this(x = value.x, w = value.y) }
    var yz: Vec2 get() = Vec2(y, z); set(value) { this(y = value.x, z = value.y) }
    var yw: Vec2 get() = Vec2(y, w); set(value) { this(y = value.x, w = value.y) }
    var zx: Vec2 get() = Vec2(z, x); set(value) { this(z = value.x, x = value.y) }
    var zw: Vec2 get() = Vec2(z, w); set(value) { this(z = value.x, w = value.y) }
    var wy: Vec2 get() = Vec2(w, y); set(value) { this(w = value.x, y = value.y) }
    var xyz: Vec3 get() = Vec3(x, y, z); set(value) { this(x = value.x, y = value.y, z = value.z) }
    var xyw: Vec3 get() = Vec3(x, y, w); set(value) { this(x = value.x, y = value.y, w = value.z) }
    var xzw: Vec3 get() = Vec3(x, z, w); set(value) { this(x = value.x, z = value.y, w = value.z) }
    var yzw: Vec3 get() = Vec3(y, z, w); set(value) { this(y = value.x, z = value.y, w = value.z) }

    var r = x
    var g = y
    var b = z
    var a = w

    override val length get() = sqrt(x * x + y * y + z * z + w * w)

    override val normal get() = length.let { if (it > 0) this / it else Vec4(0) }
    override val normalUnsafe get() = this / length

    constructor() : this(0)
    constructor(s: Number) : this(s, s, s, s)
    constructor(x: Number, y: Number, z: Number, w: Number) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(v: Vec2c, z: Number, w: Number) : this(v.x, v.y, z, w)
    constructor(v: Vec3c, w: Number) : this(v.x, v.y, v.z, w)
    constructor(v: Vec4c) : this(v.x, v.y, v.z, v.w)

    override infix fun dot(v: Vec4c) = x * v.x + y * v.y + z * v.z + w * v.w
    override fun cross(v: Vec4c) = TODO("Not yet implemented")
    
    override fun lerp(other: Vec4c, t: Float) = TODO("Not yet implemented")

    override fun minComponent() =
        if (x < y) {
            if (x < z) { if (x < w) 0 else 3 }
            else { if (z < w) 2 else 3 }
        } else {
            if (y < z) { if (y < w) 1 else 3 }
            else { if (z < w) 2 else 3 }
        }
    override fun maxComponent() =
        if (x > y) {
            if (x > z) { if (x > w) 0 else 3 }
            else { if (z > w) 2 else 3 }
        } else {
            if (y > z) { if (y > w) 1 else 3 }
            else { if (z > w) 2 else 3 }
        }

    override operator fun get(i: Int) = when (i) {
        0 -> x; 1 -> y; 2 -> z; 3 -> w; else -> error("Number $i is not a valid component for Vec4")
    }

    override operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); 2 -> z = s.toFloat(); 3 -> w = s.toFloat()
        else -> error("Number $i is not a valid component for Vec4")
    }

    operator fun invoke(v: Vec4c) = this(v.x, v.y, v.z, v.w)
    operator fun invoke(s: Number) = this(s, s, s, s)
    operator fun invoke(x: Number? = null, y: Number? = null, z: Number? = null, w: Number? = null): Vec4 {
        x?.let { this.x = it.toFloat() }
        y?.let { this.y = it.toFloat() }
        z?.let { this.z = it.toFloat() }
        w?.let { this.w = it.toFloat() }
        return this
    }

    override fun equals(other: Any?) = other is Vec4 && x == other.x && y == other.y && z == other.z && w == other.w

    override fun iterator(): Iterator<Float> = object : Iterator<Float> {
        var i = 0
        override fun next() = get(i++)
        override fun hasNext() = i < 4
    }

    override fun toInt() = Vec4i(x.toInt(), y.toInt(), z.toInt(), w.toInt())
}
// @formatter:on

interface Vec4ic : Iterable<Int> {
    val x: Int
    val y: Int
    val z: Int
    val w: Int

    operator fun get(i: Int): Int
    operator fun set(i: Int, s: Number)
}

data class Vec4i(override var x: Int, override var y: Int, override var z: Int, override var w: Int) : Vec4ic {
    constructor(s: Int) : this(s, s, s, s)

    override fun get(i: Int): Int = when (i) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> error("Number $i is not a valid component for Vec4i")
    }

    override fun set(i: Int, s: Number): Unit = when (i) {
        0 -> x = s.toInt()
        1 -> y = s.toInt()
        2 -> z = s.toInt()
        3 -> w = s.toInt()
        else -> error("Number $i is not a valid component for Vec4i")
    }

    override fun iterator() = TODO("Not yet implemented")
}

// m12 -> mij, in column major, i is row and j is column, and values are filled and accessed (for vector access) along columns
// so this is, like, stupid maybe, but separate fields like this are not necessarily contiguous in memory, right? array instead?

// @formatter:off
interface Mat2c {
    val m00: Float; val m10: Float
    val m01: Float; val m11: Float

    fun rotate(angle: Float)
}
// @formatter:on

//primary constructor is in column major
data class Mat2(
    override var m00: Float, override var m10: Float,
    override var m01: Float, override var m11: Float
) : Mat2c {
    //single argument is applied on main diagonal, and creates identity by default
    constructor(s: Number = 1) : this(s, s)

    //main diagonal constructor
    constructor(m00: Number, m11: Number) : this(
        m00.toFloat(), 0f,
        0f, m11.toFloat()
    )

    //vector constructors
    constructor(diag: Vec2) : this(diag[0], diag[1])
    constructor(c0: Vec2, c1: Vec2) : this(c0[0], c0[1], c1[0], c1[1])

    constructor(mat: Mat2) : this(mat.m00, mat.m10, mat.m01, mat.m11)

    val transposed get() = Mat2(this).transpose()
    fun transpose() = this(m00, m01, m10, m11)

    override fun rotate(angle: Float): Unit = TODO("Not yet implemented")

    // @formatter:off
    operator fun get(i: Int, j: Int) = when (i) {
        0 -> when (j) { 0 -> m00; 1 -> m01; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10; 1 -> m11; else -> indexError(i, j) }
        else -> indexError(i, j)
    }

    operator fun get(j: Int) = get(j, Vec2())
    operator fun get(j: Int, dest: Vec2) = when (j) {
        0 -> dest(m00, m10)
        1 -> dest(m01, m11)
        else -> error("Column index $j is not valid for Mat2")
    }

    operator fun set(i: Int, j: Int, s: Number) = s.toFloat().also { s -> when (i) {
        0 -> when (j) { 0 -> m00 = s; 1 -> m01 = s; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10 = s; 1 -> m11 = s; else -> indexError(i, j) }
        else -> indexError(i, j)
    } }

    operator fun set(j: Int, col: Vec2) = when (j) {
        0 -> { m00 = col[0]; m10 = col[1] }
        1 -> { m01 = col[0]; m11 = col[1] }
        else -> error("Column index $j is not valid for Mat2")
    }
    // @formatter:on

    private fun indexError(i: Int, j: Int): Nothing = error("Indices [$i, $j] are not valid for Mat2")

    //shorthand "setters"
    operator fun invoke(s: Number) = this(s, 0, 0, s)
    operator fun invoke(m00: Number, m10: Number, m01: Number, m11: Number): Mat2 {
        this.m00 = m00.toFloat(); this.m01 = m01.toFloat()
        this.m10 = m10.toFloat(); this.m11 = m11.toFloat()
        return this
    }
}

interface Mat3c {
    // @formatter:off
    val m00: Float; val m10: Float; val m20: Float
    val m01: Float; val m11: Float; val m21: Float
    val m02: Float; val m12: Float; val m22: Float
    // @formatter:on

    val inverted: Mat3

    val transposed: Mat3
}

data class Mat3(
    override var m00: Float, override var m10: Float, override var m20: Float,
    override var m01: Float, override var m11: Float, override var m21: Float,
    override var m02: Float, override var m12: Float, override var m22: Float
) : Mat3c {
    constructor(s: Number = 1) : this(s, s, s)
    constructor(m00: Number, m11: Number, m22: Number) : this(
        m00.toFloat(), 0f, 0f,
        0f, m11.toFloat(), 0f,
        0f, 0f, m22.toFloat()
    )

    constructor(diag: Vec3) : this(diag[0], diag[1], diag[2])
    constructor(c0: Vec3, c1: Vec3, c2: Vec3) : this(
        c0[0], c0[1], c0[2],
        c1[0], c1[1], c1[2],
        c2[0], c2[1], c2[2]
    )

    constructor(mat: Mat3) : this(mat.m00, mat.m10, mat.m20, mat.m01, mat.m11, mat.m21, mat.m02, mat.m12, mat.m22)
    constructor(mat: Mat4) : this(mat.m00, mat.m10, mat.m20, mat.m01, mat.m11, mat.m21, mat.m02, mat.m12, mat.m22)

    override val inverted get() = Mat3(this).invert()
    fun invert(): Mat3 {
        val a = m00 * m11 + (-m01 * m10)
        val b = m02 * m10 + (-m00 * m12)
        val c = m01 * m12 + (-m02 * m11)
        val d = a * m22 + (b * m21 + (c * m20))
        val s = 1f / d

        val nm00 = (m11 * m22 + (-m21 * m12)) * s
        val nm01 = (m21 * m02 + (-m01 * m22)) * s
        val nm02 = c * s
        val nm10 = (m20 * m12 + (-m10 * m22)) * s
        val nm11 = (m00 * m22 + (-m20 * m02)) * s
        val nm12 = b * s
        val nm20 = (m10 * m21 + (-m20 * m11)) * s
        val nm21 = (m20 * m01 + (-m00 * m21)) * s
        val nm22 = a * s

        return this(nm00, nm10, nm20, nm01, nm11, nm21, nm02, nm12, nm22)
    }

    override val transposed get() = Mat3(this).transpose()
    fun transpose() = this(m00, m01, m02, m10, m11, m12, m20, m21, m22)

    // @formatter:off
    operator fun get(i: Int, j: Int) = when (i) {
        0 -> when (j) { 0 -> m00; 1 -> m01; 2 -> m02; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10; 1 -> m11; 2 -> m12; else -> indexError(i, j) }
        2 -> when (j) { 0 -> m20; 1 -> m21; 2 -> m22; else -> indexError(i, j) }
        else -> indexError(i, j)
    }

    operator fun get(j: Int) = get(j, Vec3())
    operator fun get(j: Int, dest: Vec3) = when (j) {
        0 -> dest(m00, m10, m20)
        1 -> dest(m01, m11, m21)
        2 -> dest(m02, m12, m22)
        else -> error("Column index $j is not valid for Mat3")
    }

    operator fun set(i: Int, j: Int, s: Number) = s.toFloat().also { s -> when (i) {
        0 -> when (j) { 0 -> m00 = s; 1 -> m01 = s; 2 -> m02; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10 = s; 1 -> m11 = s; 2 -> m12;  else -> indexError(i, j) }
        2 -> when (j) { 0 -> m20 = s; 1 -> m21 = s; 2 -> m22;  else -> indexError(i, j) }
        else -> indexError(i, j)
    } }

    operator fun set(j: Int, vec: Vec3) = when (j) {
        0 -> { m00 = vec[0]; m10 = vec[1]; m20 = vec[2] }
        1 -> { m01 = vec[0]; m11 = vec[1]; m21 = vec[2] }
        2 -> { m02 = vec[0]; m12 = vec[1]; m22 = vec[2] }
        else -> error("Column index $j is not valid for Mat3")
    }
    // @formatter:on

    private fun indexError(i: Int, j: Int): Nothing = error("Indices [$i, $j] are not valid for Mat3")

    operator fun invoke(s: Number) = this(s, 0, 0, 0, s, 0, 0, 0, s)
    operator fun invoke(
        m00: Number, m10: Number, m20: Number,
        m01: Number, m11: Number, m21: Number,
        m02: Number, m12: Number, m22: Number
    ): Mat3 {
        this.m00 = m00.toFloat(); this.m01 = m01.toFloat(); this.m02 = m02.toFloat()
        this.m10 = m10.toFloat(); this.m11 = m11.toFloat(); this.m12 = m12.toFloat()
        this.m20 = m20.toFloat(); this.m21 = m21.toFloat(); this.m22 = m22.toFloat()
        return this
    }
}

// @formatter:off
interface Mat4c {
    var m00: Float; var m10: Float; var m20: Float; var m30: Float
    var m01: Float; var m11: Float; var m21: Float; var m31: Float
    var m02: Float; var m12: Float; var m22: Float; var m32: Float
    var m03: Float; var m13: Float; var m23: Float; var m33: Float

    val inverted: Mat4

    val normal: Mat3

    val translation: Vec3
    val rotation: Quat
    val scaling: Vec3

    infix fun translate(v: Vec3c): Mat4
    infix fun rotate(q: Quatc): Mat4
    infix fun scale(s: Vec3c): Mat4

    /**
     * @return `this` * [v], where [v] is viewed as a position, i.e. as if `v.w` were `1`.
     */
    operator fun times(v: Vec3c): Vec3
    /**
     * @return `this` * [v], where [v] is viewed as a direction, i.e. as if `v.w` were `0`.
     */
    fun timesDirection(v: Vec3c): Vec3
    operator fun times(v: Vec4c): Vec4
    operator fun times(m: Mat4c): Mat4
    operator fun times(aabb: AABBc): AABB

    fun frustumTestSphere(center: Vec3c, radius: Float): Boolean
    fun frustumTestAABB(aabb: AABBc): Boolean

    fun toMutable() = Mat4(this)
}
// @formatter:on

data class Mat4(
    override var m00: Float, override var m10: Float, override var m20: Float, override var m30: Float,
    override var m01: Float, override var m11: Float, override var m21: Float, override var m31: Float,
    override var m02: Float, override var m12: Float, override var m22: Float, override var m32: Float,
    override var m03: Float, override var m13: Float, override var m23: Float, override var m33: Float
) : Mat4c {
    constructor(s: Number = 1) : this(s, s, s, s)
    constructor(m00: Number, m11: Number, m22: Number, m33: Number) : this(
        m00.toFloat(), 0f, 0f, 0f,
        0f, m11.toFloat(), 0f, 0f,
        0f, 0f, m22.toFloat(), 0f,
        0f, 0f, 0f, m33.toFloat()
    )

    constructor(diag: Vec4c) : this(diag[0], diag[1], diag[2], diag[3])
    constructor(c0: Vec4c, c1: Vec4c, c2: Vec4c, c3: Vec4c) : this(
        c0[0], c0[1], c0[2], c0[3],
        c1[0], c1[1], c1[2], c1[3],
        c2[0], c2[1], c2[2], c2[3],
        c3[0], c3[1], c3[2], c3[3]
    )

    constructor(mat: Mat4c) : this(
        mat.m00, mat.m10, mat.m20, mat.m30,
        mat.m01, mat.m11, mat.m21, mat.m31,
        mat.m02, mat.m12, mat.m22, mat.m32,
        mat.m03, mat.m13, mat.m23, mat.m33
    )

    companion object {
        val IDENTITY = Mat4().toConst()
    }

    val transposed get() = Mat4(this).transpose()
    fun transpose() = this(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33)

    override val inverted: Mat4
        get() {
            val a = m00 * m11 - m01 * m10;
            val b = m00 * m12 - m02 * m10;
            val c = m00 * m13 - m03 * m10
            val d = m01 * m12 - m02 * m11;
            val e = m01 * m13 - m03 * m11;
            val f = m02 * m13 - m03 * m12
            val g = m20 * m31 - m21 * m30;
            val h = m20 * m32 - m22 * m30;
            val i = m20 * m33 - m23 * m30
            val j = m21 * m32 - m22 * m31;
            val k = m21 * m33 - m23 * m31;
            val l = m22 * m33 - m23 * m32
            val det = 1.0f / (a * l - b * k + c * j + d * i - e * h + f * g)
            return Mat4(
                m00 = (m11 * l + (-m12 * k + (m13 * j))) * det,
                m01 = (-m01 * l + (m02 * k + (-m03 * j))) * det,
                m02 = (m31 * f + (-m32 * e + (m33 * d))) * det,
                m03 = (-m21 * f + (m22 * e + (-m23 * d))) * det,
                m10 = (-m10 * l + (m12 * i + (-m13 * h))) * det,
                m11 = (m00 * l + (-m02 * i + (m03 * h))) * det,
                m12 = (-m30 * f + (m32 * c + (-m33 * b))) * det,
                m13 = (m20 * f + (-m22 * c + (m23 * b))) * det,
                m20 = (m10 * k + (-m11 * i + (m13 * g))) * det,
                m21 = (-m00 * k + (m01 * i + (-m03 * g))) * det,
                m22 = (m30 * e + (-m31 * c + (m33 * a))) * det,
                m23 = (-m20 * e + (m21 * c + (-m23 * a))) * det,
                m30 = (-m10 * j + (m11 * h + (-m12 * g))) * det,
                m31 = (m00 * j + (-m01 * h + (m02 * g))) * det,
                m32 = (-m30 * d + (m31 * b + (-m32 * a))) * det,
                m33 = (m20 * d + (-m21 * b + (m22 * a))) * det,
            )
        }

    override val normal: Mat3 get() = Mat3(this).invert().transpose()

    override val translation: Vec3 get() = Vec3(m30, m31, m32)
    override val rotation: Quat
        get() = Quat().apply {
            val lenX = 1f / sqrt(m00 * m00 + m01 * m01 + m02 * m02)
            val lenY = 1f / sqrt(m10 * m10 + m11 * m11 + m12 * m12)
            val lenZ = 1f / sqrt(m20 * m20 + m21 * m21 + m22 * m22)
            val m00 = m00 / lenX
            val m01 = m01 / lenX
            val m02 = m02 / lenX
            val m10 = m10 / lenY
            val m11 = m11 / lenY
            val m12 = m12 / lenY
            val m20 = m20 / lenZ
            val m21 = m21 / lenZ
            val m22 = m22 / lenZ

            var t: Float
            val tr = m00 + m11 + m22
            when {
                tr >= 0f -> {
                    t = sqrt(tr + 1f)
                    w = t * 0.5f
                    t = 0.5f / t
                    x = (m12 - m21) * t
                    y = (m20 - m02) * t
                    z = (m01 - m10) * t
                }

                m00 >= m11 && m00 >= m22 -> {
                    t = sqrt(m00 - (m11 + m22) + 1f)
                    x = t * 0.5f
                    t = 0.5f / t
                    y = (m10 + m01) * t
                    z = (m02 + m20) * t
                    w = (m10 - m21) * t
                }

                m11 > m22 -> {
                    t = sqrt(m11 - (m22 + m00) + 1f)
                    y = t * 0.5f
                    t = 0.5f / t
                    z = (m21 + m12) * t
                    x = (m10 + m01) * t
                    w = (m20 - m02) * t
                }

                else -> {
                    t = sqrt(m22 - (m00 + m11) + 1f)
                    z = t * 0.5f
                    t = 0.5f / t
                    x = (m02 + m20) * t
                    y = (m21 + m12) * t
                    w = (m01 - m10) * t
                }
            }
        }

    override val scaling: Vec3
        get() = Vec3(
            sqrt(m00 * m00 + m01 * m01 + m02 * m02),
            sqrt(m10 * m10 + m11 * m11 + m12 * m12),
            sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        )

    override infix fun translate(v: Vec3c): Mat4 {
        m30 = m00 * v.x + (m10 * v.y + (m20 * v.z + m30))
        m31 = m01 * v.x + (m11 * v.y + (m21 * v.z + m31))
        m32 = m02 * v.x + (m12 * v.y + (m22 * v.z + m32))
        m33 = m03 * v.x + (m13 * v.y + (m23 * v.z + m33))
        return this
    }

    override infix fun rotate(q: Quatc): Mat4 {
        val w2 = q.w * q.w;
        val x2 = q.x * q.x;
        val y2 = q.y * q.y;
        val z2 = q.z * q.z
        val zw = q.z * q.w;
        val dzw = zw + zw;
        val xy = q.x * q.y;
        val dxy = xy + xy
        val xz = q.x * q.z;
        val dxz = xz + xz;
        val yw = q.y * q.w;
        val dyw = yw + yw
        val yz = q.y * q.z;
        val dyz = yz + yz;
        val xw = q.x * q.w;
        val dxw = xw + xw

        val rm00 = w2 + x2 - z2 - y2;
        val rm01 = dxy + dzw;
        val rm02 = dxz - dyw
        val rm10 = -dzw + dxy;
        val rm11 = y2 - z2 + w2 - x2;
        val rm12 = dyz + dxw
        val rm20 = dyw + dxz;
        val rm21 = dyz - dxw;
        val rm22 = z2 - y2 - x2 + w2

        val nm00 = m00 * rm00 + (m10 * rm01 + (m20 * rm02))
        val nm01 = m01 * rm00 + (m11 * rm01 + (m21 * rm02))
        val nm02 = m02 * rm00 + (m12 * rm01 + (m22 * rm02))
        val nm03 = m03 * rm00 + (m13 * rm01 + (m23 * rm02))
        val nm10 = m00 * rm10 + (m10 * rm11 + (m20 * rm12))
        val nm11 = m01 * rm10 + (m11 * rm11 + (m21 * rm12))
        val nm12 = m02 * rm10 + (m12 * rm11 + (m22 * rm12))
        val nm13 = m03 * rm10 + (m13 * rm11 + (m23 * rm12))

        m20 = m00 * rm20 + (m10 * rm21 + (m20 * rm22))
        m20 = m00 * rm20 + (m10 * rm21 + (m20 * rm22))
        m21 = m01 * rm20 + (m11 * rm21 + (m21 * rm22))
        m22 = m02 * rm20 + (m12 * rm21 + (m22 * rm22))
        m23 = m03 * rm20 + (m13 * rm21 + (m23 * rm22))
        m00 = nm00; m01 = nm01; m02 = nm02; m03 = nm03
        m10 = nm10; m11 = nm11; m12 = nm12; m13 = nm13
        return this
    }

    override infix fun scale(s: Vec3c): Mat4 {
        m00 *= s.x; m01 *= s.x; m02 *= s.x; m03 *= s.x
        m10 *= s.y; m11 *= s.y; m12 *= s.y; m13 *= s.y
        m20 *= s.z; m21 *= s.z; m22 *= s.z; m23 *= s.z
        return this
    }

    fun setLookAt(eye: Vec3c, center: Vec3c, up: Vec3c) {
        var dirX = eye.x - center.x
        var dirY = eye.y - center.y
        var dirZ = eye.z - center.z
        val invDirLength = 1f / sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength; dirY *= invDirLength; dirZ *= invDirLength

        var leftX = up.y * dirZ - up.z * dirY
        var leftY = up.z * dirX - up.x * dirZ
        var leftZ = up.x * dirY - up.y * dirX
        val invLeftLength = 1f / sqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength; leftY *= invLeftLength; leftZ *= invLeftLength

        val upX = dirY * leftZ - dirZ * leftY
        val upY = dirZ * leftX - dirX * leftZ
        val upZ = dirX * leftY - dirY * leftX

        this(
            m00 = leftX, m01 = upX, m02 = dirX, m03 = 0f,
            m10 = leftY, m11 = upY, m12 = dirY, m13 = 0f,
            m20 = leftZ, m21 = upZ, m22 = dirZ, m23 = 0f,
            m30 = -(leftX * eye.x + leftY * eye.y + leftZ * eye.z),
            m31 = -(upX * eye.x + upY * eye.y + upZ * eye.z),
            m32 = -(dirX * eye.x + dirY * eye.y + dirZ * eye.z),
            m33 = 1f
        )
    }

    fun setOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) {
        this()

        m00 = 2f / (right - left)
        m11 = 2f / (top - bottom)
        m22 = 2f / (near - far)
        m30 = (right + left) / (left - right)
        m31 = (top + bottom) / (bottom - top)
        m32 = (far + near) / (near + far)
    }

    fun setPerspective(
        verticalFieldOfView: Float,
        aspectRatio: Float,
        near: Float,
        far: Float,
        depthZeroToOne: Boolean = false
    ) {
        invoke(0)
        val h = tan(verticalFieldOfView * 0.5f)
        m00 = 1.0f / (h * aspectRatio)
        m11 = 1.0f / h
        val farInf = far > 0 && far.isInfinite()
        val nearInf = near > 0 && near.isInfinite()
        if (farInf) {
            // See: "Infinite Projection Matrix" (http://www.terathon.com/gdc07_lengyel.pdf)
            val e = 1E-6f
            m22 = e - 1.0f
            m32 = (e - (if (depthZeroToOne) 1.0f else 2.0f)) * near
        } else if (nearInf) {
            val e = 1E-6f
            m22 = (if (depthZeroToOne) 0.0f else 1.0f) - e
            m32 = ((if (depthZeroToOne) 1.0f else 2.0f) - e) * far
        } else {
            m22 = (if (depthZeroToOne) far else far + near) / (near - far)
            m32 = (if (depthZeroToOne) far else far + far) * near / (near - far)
        }
        m23 = -1.0f
    }

    override fun times(v: Vec3c): Vec3 = Vec3(
        v.x * m00 + v.y * m01 + v.z * m02 + m03,
        v.x * m10 + v.y * m11 + v.z * m12 + m13,
        v.x * m20 + v.y * m21 + v.z * m22 + m23
    )

    override fun timesDirection(v: Vec3c): Vec3 = Vec3(
        v.x * m00 + v.y * m01 + v.z * m02,
        v.x * m10 + v.y * m11 + v.z * m12,
        v.x * m20 + v.y * m21 + v.z * m22
    )

    override fun times(v: Vec4c): Vec4 = Vec4(
        v.x * m00 + v.y * m01 + v.z * m02 + v.w * m03,
        v.x * m10 + v.y * m11 + v.z * m12 + v.w * m13,
        v.x * m20 + v.y * m21 + v.z * m22 + v.w * m23,
        v.x * m30 + v.y * m31 + v.z * m32 + v.w * m33
    )

    override operator fun times(m: Mat4c): Mat4 = Mat4(
        m00 = (m00 * m.m00 + (m10 * m.m01 + (m20 * m.m02 + (m30 * m.m03)))),
        m01 = (m01 * m.m00 + (m11 * m.m01 + (m21 * m.m02 + (m31 * m.m03)))),
        m02 = (m02 * m.m00 + (m12 * m.m01 + (m22 * m.m02 + (m32 * m.m03)))),
        m03 = (m03 * m.m00 + (m13 * m.m01 + (m23 * m.m02 + (m33 * m.m03)))),
        m10 = (m00 * m.m10 + (m10 * m.m11 + (m20 * m.m12 + (m30 * m.m13)))),
        m11 = (m01 * m.m10 + (m11 * m.m11 + (m21 * m.m12 + (m31 * m.m13)))),
        m12 = (m02 * m.m10 + (m12 * m.m11 + (m22 * m.m12 + (m32 * m.m13)))),
        m13 = (m03 * m.m10 + (m13 * m.m11 + (m23 * m.m12 + (m33 * m.m13)))),
        m20 = (m00 * m.m20 + (m10 * m.m21 + (m20 * m.m22 + (m30 * m.m23)))),
        m21 = (m01 * m.m20 + (m11 * m.m21 + (m21 * m.m22 + (m31 * m.m23)))),
        m22 = (m02 * m.m20 + (m12 * m.m21 + (m22 * m.m22 + (m32 * m.m23)))),
        m23 = (m03 * m.m20 + (m13 * m.m21 + (m23 * m.m22 + (m33 * m.m23)))),
        m30 = (m00 * m.m30 + (m10 * m.m31 + (m20 * m.m32 + (m30 * m.m33)))),
        m31 = (m01 * m.m30 + (m11 * m.m31 + (m21 * m.m32 + (m31 * m.m33)))),
        m32 = (m02 * m.m30 + (m12 * m.m31 + (m22 * m.m32 + (m32 * m.m33)))),
        m33 = (m03 * m.m30 + (m13 * m.m31 + (m23 * m.m32 + (m33 * m.m33))))
    )

    override operator fun times(aabb: AABBc): AABB {
        var minX = 0f
        var minY = 0f
        var minZ = 0f
        var maxX = 0f
        var maxY = 0f
        var maxZ = 0f

        for (i in 0..<8) {
            val x = if (i % 2 == 0) aabb.minX else aabb.maxX
            val y = if ((i shr 1) % 2 == 0) aabb.minY else aabb.maxY
            val z = if ((i shr 2) % 2 == 0) aabb.minZ else aabb.maxZ

            minX = min(minX, x * m00 + y * m01 + z * m02 + m03)
            minY = min(minY, x * m10 + y * m11 + z * m12 + m13)
            minZ = min(minZ, x * m20 + y * m21 + z * m22 + m23)

            maxX = max(maxX, x * m00 + y * m01 + z * m02 + m03)
            maxY = max(maxY, x * m10 + y * m11 + z * m12 + m13)
            maxZ = max(maxZ, x * m20 + y * m21 + z * m22 + m23)
        }

        return AABB(minX, minY, minZ, maxX, maxY, maxZ)
    }

    override fun frustumTestSphere(center: Vec3c, radius: Float): Boolean = TODO("Not yet implemented")
    override fun frustumTestAABB(aabb: AABBc): Boolean = TODO("Not yet implemented")

    // @formatter:off
    operator fun get(i: Int, j: Int) = when (i) {
        0 -> when (j) { 0 -> m00; 1 -> m01; 2 -> m02; 3 -> m03; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10; 1 -> m11; 2 -> m12; 3 -> m13; else -> indexError(i, j) }
        2 -> when (j) { 0 -> m20; 1 -> m21; 2 -> m22; 3 -> m23; else -> indexError(i, j) }
        3 -> when (j) { 0 -> m30; 1 -> m31; 2 -> m32; 3 -> m33; else -> indexError(i, j) }
        else -> indexError(i, j)
    }

    operator fun get(j: Int) = get(j, Vec4())
    operator fun get(j: Int, dest: Vec4) = when (j) {
        0 -> dest(m00, m10, m20, m30)
        1 -> dest(m01, m11, m21, m31)
        2 -> dest(m02, m12, m22, m32)
        3 -> dest(m03, m13, m23, m33)
        else -> error("Column index $j is not valid for Mat4")
    }

    operator fun set(i: Int, j: Int, s: Number) = s.toFloat().also { s -> when (i) {
        0 -> when (j) { 0 -> m00 = s; 1 -> m01 = s; 2 -> m02; 3 -> m03 = s; else -> indexError(i, j) }
        1 -> when (j) { 0 -> m10 = s; 1 -> m11 = s; 2 -> m12; 3 -> m13 = s; else -> indexError(i, j) }
        2 -> when (j) { 0 -> m20 = s; 1 -> m21 = s; 2 -> m22; 3 -> m23 = s; else -> indexError(i, j) }
        3 -> when (j) { 0 -> m30 = s; 1 -> m31 = s; 2 -> m32; 3 -> m33 = s; else -> indexError(i, j) }
        else -> indexError(i, j)
    } }

    operator fun set(j: Int, vec: Vec4) = when (j) {
        0 -> { m00 = vec[0]; m10 = vec[1]; m20 = vec[2]; m30 = vec[3] }
        1 -> { m01 = vec[0]; m11 = vec[1]; m21 = vec[2]; m31 = vec[3] }
        2 -> { m02 = vec[0]; m12 = vec[1]; m22 = vec[2]; m32 = vec[3] }
        3 -> { m03 = vec[0]; m13 = vec[1]; m23 = vec[2]; m33 = vec[3] }
        else -> error("Column index $j is not valid for Mat4")
    }
    // @formatter:on

    private fun indexError(i: Int, j: Int): Nothing = error("Indices [$i, $j] are not valid for Mat4")

    operator fun invoke() = this(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)
    operator fun invoke(s: Number) = this(s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, s)
    operator fun invoke(m: Mat4c) = this(
        m.m00, m.m10, m.m20, m.m30,
        m.m01, m.m11, m.m21, m.m31,
        m.m02, m.m12, m.m22, m.m32,
        m.m03, m.m13, m.m23, m.m33
    )

    operator fun invoke(
        m00: Number, m10: Number, m20: Number, m30: Number,
        m01: Number, m11: Number, m21: Number, m31: Number,
        m02: Number, m12: Number, m22: Number, m32: Number,
        m03: Number, m13: Number, m23: Number, m33: Number
    ): Mat4 {
        this.m00 = m00.toFloat(); this.m01 = m01.toFloat(); this.m02 = m02.toFloat(); this.m03 = m03.toFloat()
        this.m10 = m10.toFloat(); this.m11 = m11.toFloat(); this.m12 = m12.toFloat(); this.m13 = m13.toFloat()
        this.m20 = m20.toFloat(); this.m21 = m21.toFloat(); this.m22 = m22.toFloat(); this.m23 = m23.toFloat()
        this.m30 = m30.toFloat(); this.m31 = m31.toFloat(); this.m32 = m32.toFloat(); this.m33 = m33.toFloat()
        return this
    }

    fun toConst() = Mat4(this) as Mat4c
}

interface Quatc {
    val x: Float
    val y: Float
    val z: Float
    val w: Float

    /**
     * Get the rotation this [Quat] describes in angles about the Y-, X-, and Z-axis, deconstructed in that order.
     */
    val eulerAnglesYXZ: Vec3c

    val isFinite: Boolean

    fun slerp(other: Quatc, t: Float): Quat

    operator fun times(q: Quatc): Quat
    operator fun times(v: Vec3c): Vec3
}

data class Quat(
    override var x: Float,
    override var y: Float,
    override var z: Float,
    override var w: Float
) : Quatc {
    /**
     * Represents the rotation this [Quat] describes in angles about the Y-, X-, and Z-axis, deconstructed in that order.
     *
     * > **Beware:** when setting this, the rotations must still be put in the components sharing their name with the
     * corresponding axis; only the order they are applied in is different; the [Vec3.x] component of [eulerAnglesYXZ]
     * contains the angle about the X-axis, [Vec3.y] for the Y-axis, and [Vec3.z] for the Z-axis, respectively.
     */
    override var eulerAnglesYXZ: Vec3c
        get() {
            val asin = -2f * (y * z - w * x)
            _eulerAnglesYXZ.x = if (asin <= -1f) -PI_OVER_2_f else (if (asin >= 1f) PI_OVER_2_f else asin(asin))
            _eulerAnglesYXZ.y = atan2(x * z + y * w, 0.5f - y * y - x * x)
            _eulerAnglesYXZ.z = atan2(y * x + w * z, 0.5f - x * x - z * z)
            return _eulerAnglesYXZ
        }
        set(value) {
            val sx = sin(value.x * 0.5f)
            val cx = cos(value.x * 0.5f)
            val sy = sin(value.y * 0.5f)
            val cy = cos(value.y * 0.5f)
            val sz = sin(value.z * 0.5f)
            val cz = cos(value.z * 0.5f)

            val x = cy * sx
            val y = sy * cx
            val z = sy * sx
            val w = cy * cx

            this.x = x * cz + y * sz
            this.y = y * cz - x * sz
            this.z = w * sz - z * cz
            this.w = w * cz + z * sz

            //TODO move this to the mutating `rotate` method
//            val yx = cy * sx
//            val yy = sy * cx
//            val yz = sy * cx
//            val yw = cy * cx
//
//            val x = yx * cz + yy * sz
//            val y = yy * cz - yx * sz
//            val z = yw * sz - yz * cz
//            val w = yw * cz + yz * sz
//
//            this.x = this.w * x + (this.x * w + (this.y * z + (-this.z * y)))
//            this.y = this.w * y + (-this.x * z + (this.y * w + (this.z * x)))
//            this.x = this.w * z + (this.x * y + (-this.y * x + (this.z * w)))
//            this.x = this.w * w + (-this.x * x + (-this.y * y + (-this.z * z)))
        }
    private val _eulerAnglesYXZ = Vec3()

    override val isFinite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()

    constructor() : this(0f, 0f, 0f, 1f)
    constructor(q: Quat) : this(q.x, q.y, q.z, q.w)

    override fun slerp(other: Quatc, t: Float): Quat {
        val cosom = x * other.x + (y * other.y + (z * other.z + (w * other.w)))
        val absCosom = abs(cosom)

        val scale0: Float
        var scale1: Float
        if (1f - absCosom > 1e-6f) {
            val sinSqr = 1f - absCosom * absCosom
            val sinom = 1f / sqrt(sinSqr)
            val omega = atan2(sinSqr * sinom, absCosom)
            scale0 = sin((1f - t) * omega) * sinom
            scale1 = sin(t * omega) * sinom
        } else {
            scale0 = 1f - t
            scale1 = t
        }
        scale1 = if (cosom >= 0.0) scale1 else -scale1

        return Quat(
            x = scale0 * x + (scale1 * x),
            y = scale0 * y + (scale1 * y),
            z = scale0 * z + (scale1 * z),
            w = scale0 * w + (scale1 * w)
        )
    }

    override operator fun times(v: Vec3c): Vec3 {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val ww = w * w
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val zw = z * w
        val yw = y * w
        val k = 1f / (xx + yy + zz + ww)
        return Vec3(
            ((xx - yy - zz + ww) * k * v.x) + ((2 * (xy - zw) * k * v.y) + (2 * (xz + yw) * k * v.z)),
            (2 * (xy + zw) * k * v.x) + (((yy - xx - zz + ww) * k * v.y) + (2 * (yz - xw) * k * v.z)),
            (2 * (xz - yw) * k * v.x) + (((2 * yz + xw) * k * v.y) + ((zz - xx - yy + ww) * k * v.z))
        )
    }

    override fun times(q: Quatc): Quat = Quat(
        w * q.x + (x * q.w + (y * q.z + (-z * q.y))),
        w * q.y + (-x * q.z + (y * q.w + (z * q.x))),
        w * q.z + (x * q.y + (-y * q.x + (z * q.w))),
        w * q.w + (-x * q.x + (-y * q.y + (-z * q.z)))
    )

    operator fun invoke(): Quat {
        this.x = 0f
        this.y = 0f
        this.z = 0f
        this.w = 1f
        return this
    }

    operator fun invoke(other: Quatc): Quat {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.w = other.w
        return this
    }
}

interface AABBc {
    val minX: Float
    val minY: Float
    val minZ: Float
    val maxX: Float
    val maxY: Float
    val maxZ: Float

    val center: Vec3
    val size: Vec3

    infix fun intersects(other: AABBc): Boolean
    infix fun intersect(other: AABBc): AABB

    /**
     * @return a copy of `this` expanded to include [point]
     */
    operator fun plus(point: Vec3c): AABB

    /**
     * @return the *union* of `this` and [other]
     */
    operator fun plus(other: AABBc): AABB
}

open class AABB(
    override var minX: Float, override var minY: Float, override var minZ: Float,
    override var maxX: Float, override var maxY: Float, override var maxZ: Float
) : AABBc {
    constructor() : this(0f, 0f, 0f, 0f, 0f, 0f)
    constructor(min: Vec3, max: Vec3) : this(min.x, min.y, min.z, max.x, max.y, max.z)
    constructor(aabb: AABBc) : this(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)

    override val center: Vec3
        get() = Vec3(minX + ((maxX - minX) / 2), minY + ((maxY - minY) / 2), minZ + ((maxZ - minZ) / 2))
    override val size: Vec3 get() = Vec3(maxX - minX, maxY - minY, maxZ - minZ)

    override fun intersects(other: AABBc): Boolean = TODO("Not yet implemented")
    override fun intersect(other: AABBc): AABB = TODO("Not yet implemented")

    override operator fun plus(point: Vec3c): AABB = AABB(
        min(minX, point.x), min(minY, point.y), min(minZ, point.z),
        max(maxX, point.x), max(maxY, point.y), max(maxZ, point.z)
    )

    override operator fun plus(other: AABBc): AABB = AABB(
        min(minX, other.minX), min(minY, other.minY), min(minZ, other.minZ),
        max(maxX, other.maxX), max(maxY, other.maxY), max(maxZ, other.maxZ)
    )

    operator fun invoke(aabb: AABBc) {
        minX = aabb.minX
        minY = aabb.minY
        minZ = aabb.minZ
        maxX = aabb.maxX
        maxY = aabb.maxY
        maxZ = aabb.maxZ
    }
}

data class Ray(
    val originX: Float, val originY: Float, val originZ: Float,
    val directionX: Float, val directionY: Float, val directionZ: Float
) {
    constructor(origin: Vec3c, direction: Vec3c) : this(
        origin.x, origin.y, origin.z, direction.x, direction.y, direction.z
    )

    constructor(frustum: Mat4c, x: Float, y: Float) : this(
        getFrustumRayOrigin(frustum),
        getFrustumRayDirection(x, y, frustum)
    )

    companion object {
        fun getFrustumRayOrigin(frustum: Mat4c): Vec3 = TODO("Not yet implemented")
        fun getFrustumRayDirection(x: Float, y: Float, frustum: Mat4c): Vec3 = TODO("Not yet implemented")
    }
}

data class Rect(val x: Float, val y: Float, val width: Float, val height: Float)
data class Recti(val x: Int, val y: Int, val width: Int, val height: Int)
