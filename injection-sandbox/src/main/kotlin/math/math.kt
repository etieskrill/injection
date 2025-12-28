package injection.sandbox.math

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val Number.radians get() = toFloat()
val Number.rad get() = radians

val Number.degrees get() = toFloat() / (PI / 180)
val Number.deg get() = degrees

val Number.turn get() = toFloat() / (2 * PI)
val Number.tr get() = turn

val Number.tau get() = 2 * toFloat()

// @formatter:off
data class Vec2(var x: Float, var y: Float) : Iterable<Float> {
    var xy: Vec2 get() = Vec2(this); set(value) { this(value) }
    var yx: Vec2 get() = Vec2(y, x); set(value) { this(y = value.x, x = value.y) }
    val xx: Vec2 get() = Vec2(x, x)//; set(value) = this(x = value.x) //TODO i don't think setters make sense here, like, which value to take?
    val yy: Vec2 get() = Vec2(y, y) //this is gonna suck for Vec4

    var u = x
    var v = y
    var vu: Vec2 get() = yx; set(value) = run { yx = value }

    val length get() = sqrt(x * x + y * y)
    val normal get() = length.let { if (it > 0) this / it else Vec2(0) }
    val normalUnsafe get() = this / length

    constructor() : this(0)
    constructor(s: Number) : this(s, s)
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())
    constructor(v: Vec2) : this(v.x, v.y)
    constructor(v: Vec3) : this(v.x, v.y)

    fun normalize(): Vec2 {
        val length = length
        if (length == 0f) return apply { this(0f) }
        x /= length
        y /= length
        return this
    }

    fun normalizeUnsafe(): Vec2 {
        val length = length
        x /= length
        y /= length
        return this
    }

    infix fun dot(v: Vec2) = x * v.x + y * v.y

    fun minComponent() = min(x, y)
    fun maxComponent() = max(x, y)

    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)
    operator fun plus(s: Number) = Vec2(x + s.toFloat(), y + s.toFloat())
    operator fun plusAssign(v: Vec2) { this(x + v.x, y + v.y) }
    operator fun plusAssign(s: Number) { this(x + s.toFloat(), y + s.toFloat()) }
    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)
    operator fun minus(s: Number) = Vec2(x - s.toFloat(), y - s.toFloat())
    operator fun minusAssign(v: Vec2) { this(x - v.x, y - v.y) }
    operator fun minusAssign(s: Number) { this(x - s.toFloat(), y - s.toFloat()) }
    operator fun unaryMinus() = Vec2(-x, -y)
    operator fun times(v: Vec2) = Vec2(x * v.x, y * v.y)
    operator fun times(s: Number) = Vec2(x * s.toFloat(), y * s.toFloat())
    operator fun timesAssign(v: Vec2) { this(x * v.x, y * v.y) }
    operator fun timesAssign(s: Number) { this(x * s.toFloat(), y * s.toFloat()) }
    operator fun div(v: Vec2) = Vec2(x / v.x, y / v.y)
    operator fun div(s: Number) = Vec2(x / s.toFloat(), y / s.toFloat())
    operator fun divAssign(v: Vec2) { this(x / v.x, y / v.y) }
    operator fun divAssign(s: Number) { this(x / s.toFloat(), y / s.toFloat()) }

    operator fun get(i: Int): Float = when (i) {
        0 -> x; 1 -> y; else -> error("Number $i is not a valid component for Vec2")
    }

    operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); else -> error("Number $i is not a valid component for Vec2")
    }

    operator fun invoke(v: Vec2) = this(v.x, v.y)
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
}

operator fun Number.plus(v: Vec2) = v + this
operator fun Number.minus(v: Vec2) = Vec2(this.toFloat() - v.x, this.toFloat() - v.y)
operator fun Number.times(v: Vec2) = v * this
operator fun Number.div(v: Vec2) = Vec2(this.toFloat() / v.x, this.toFloat() / v.y)

data class Vec3(var x: Float, var y: Float, var z: Float) : Iterable<Float> {
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

    val length = sqrt(x * x + y * y + z * z)
    val normal get() = length.let { if (it > 0) this / it else Vec3(0) }
    val normalUnsafe get() = this / length

    constructor() : this(0)
    constructor(s: Number) : this(s, s, s)
    constructor(x: Number, y: Number, z: Number) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(v: Vec2, z: Number) : this(v.x, v.y, z)
    constructor(v: Vec3) : this(v.x, v.y, v.z)

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

    infix fun dot(v: Vec3) = x * v.x + y * v.y + z * v.z
    infix fun cross(v: Vec3) = Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

    fun toVec2() = Vec2(this)

    fun minComponent() = min(x, min(y, z))
    fun maxComponent() = max(x, max(y, z))

    operator fun plus(v: Vec3) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun plus(s: Number) = Vec3(x + s.toFloat(), y + s.toFloat(), z + s.toFloat())
    operator fun plusAssign(v: Vec3) { this(x + v.x, y + v.y, z + v.z) }
    operator fun plusAssign(s: Number) { this(x + s.toFloat(), y + s.toFloat(), z + s.toFloat()) }
    operator fun minus(v: Vec3) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun minus(s: Number) = Vec3(x - s.toFloat(), y - s.toFloat(), z - s.toFloat())
    operator fun minusAssign(v: Vec3) { this(x - v.x, y - v.y, z - v.z) }
    operator fun minusAssign(s: Number) { this(x - s.toFloat(), y - s.toFloat(), z - s.toFloat()) }
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    operator fun times(v: Vec3) = Vec3(x * v.x, y * v.y, z * v.z)
    operator fun times(s: Number) = Vec3(x * s.toFloat(), y * s.toFloat(), z * s.toFloat())
    operator fun timesAssign(v: Vec3) { this(x * v.x, y * v.y, z * v.z) }
    operator fun timesAssign(s: Number) { this(x * s.toFloat(), y * s.toFloat(), z * s.toFloat()) }
    operator fun div(v: Vec3) = Vec3(x / v.x, y / v.y, z / v.z)
    operator fun div(s: Number) = Vec3(x / s.toFloat(), y / s.toFloat(), z / s.toFloat())
    operator fun divAssign(v: Vec3) { this(x / v.x, y / v.y, z / v.z) }
    operator fun divAssign(s: Number) { this(x / s.toFloat(), y / s.toFloat(), z / s.toFloat()) }

    operator fun get(i: Int) = when (i) {
        0 -> x; 1 -> y; 2 -> z; else -> error("Number $i is not a valid component for Vec3")
    }

    operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); 2 -> z = s.toFloat();
        else -> error("Number $i is not a valid component for Vec3")
    }

    operator fun invoke(v: Vec3) = this(v.x, v.y, v.z)
    operator fun invoke(s: Number) = this(s, s, s)
    operator fun invoke(x: Number? = null, y: Number? = null, z: Number? = null): Vec3 {
        x?.let { this.x = it.toFloat() }
        y?.let { this.y = it.toFloat() }
        z?.let { this.z = it.toFloat() }
        return this
    }

    override fun equals(other: Any?) = other is Vec3 && x == other.x && y == other.y && z == other.z

    override fun iterator(): Iterator<Float> = object : Iterator<Float> {
        var i = 0
        override fun next() = get(i++)
        override fun hasNext() = i < 3
    }
}

data class Vec4(var x: Float, var y: Float, var z: Float, var w: Float) : Iterable<Float> {
    //and these are only the combinations, meaning only 10 out of 36 permutations so far, and like 10 out of 336 (i believe) replacement permutations... sigh
    var xy: Vec2 get() = Vec2(x, y); set(value) { this(x = value.x, y = value.y) }
    var xz: Vec2 get() = Vec2(x, z); set(value) { this(x = value.x, z = value.y) }
    var xw: Vec2 get() = Vec2(x, w); set(value) { this(x = value.x, w = value.y) }
    var yz: Vec2 get() = Vec2(y, z); set(value) { this(y = value.x, z = value.y) }
    var yw: Vec2 get() = Vec2(y, w); set(value) { this(y = value.x, w = value.y) }
    var zw: Vec2 get() = Vec2(z, w); set(value) { this(z = value.x, w = value.y) }
    var xyz: Vec3 get() = Vec3(x, y, z); set(value) { this(x = value.x, y = value.y, z = value.z) }
    var xyw: Vec3 get() = Vec3(x, y, w); set(value) { this(x = value.x, y = value.y, w = value.z) }
    var xzw: Vec3 get() = Vec3(x, z, w); set(value) { this(x = value.x, z = value.y, w = value.z) }
    var yzw: Vec3 get() = Vec3(y, z, w); set(value) { this(y = value.x, z = value.y, w = value.z) }

    var r = x
    var g = y
    var b = z
    var a = w

    constructor() : this(0)
    constructor(s: Number) : this(s, s, s, s)
    constructor(x: Number, y: Number, z: Number, w: Number) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(v: Vec2, z: Number, w: Number) : this(v.x, v.y, z, w)
    constructor(v: Vec3, w: Number) : this(v.x, v.y, v.z, w)
    constructor(v: Vec4) : this(v.x, v.y, v.z, v.w)

    infix fun dot(v: Vec4) = x * v.x + y * v.y + z * v.z + w * v.w

    fun toVec2() = Vec2(x, y)
    fun toVec3() = Vec3(x, y, z)

    fun minComponent() = min(x, min(y, min(z, w)))
    fun maxComponent() = max(x, max(y, max(z, w)))

    operator fun plus(v: Vec4) = Vec4(x + v.x, y + v.y, z + v.z, w + v.w)
    operator fun plus(s: Number) = Vec4(x + s.toFloat(), y + s.toFloat(), z + s.toFloat(), w + s.toFloat())
    operator fun minus(v: Vec4) = Vec4(x - v.x, y - v.y, z - v.z, w - v.w)
    operator fun minus(s: Number) = Vec4(x - s.toFloat(), y - s.toFloat(), z - s.toFloat(), w - s.toFloat())
    operator fun unaryMinus() = Vec4(-x, -y, -z, -w)
    operator fun times(v: Vec4) = Vec4(x * v.x, y * v.y, z * v.z, w * v.w)
    operator fun times(s: Number) = Vec4(x * s.toFloat(), y * s.toFloat(), z * s.toFloat(), w * s.toFloat())
    operator fun div(v: Vec4) = Vec4(x / v.x, y / v.y, z / v.z, w / v.w)
    operator fun div(s: Number) = Vec4(x / s.toFloat(), y / s.toFloat(), z / s.toFloat(), w / s.toFloat())

    operator fun get(i: Int) = when (i) {
        0 -> x; 1 -> y; 2 -> z; 3 -> w; else -> error("Number $i is not a valid component for Vec4")
    }

    operator fun set(i: Int, s: Number) = when (i) {
        0 -> x = s.toFloat(); 1 -> y = s.toFloat(); 2 -> z = s.toFloat(); 3 -> w = s.toFloat()
        else -> error("Number $i is not a valid component for Vec4")
    }

    operator fun invoke(v: Vec4) = this(v.x, v.y, v.z, v.w)
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
}
// @formatter:on

operator fun Number.plus(v: Vec3) = v + this
operator fun Number.minus(v: Vec3) = Vec3(this.toFloat() - v.x, this.toFloat() - v.y, this.toFloat() - v.z)
operator fun Number.times(v: Vec3) = v * this
operator fun Number.div(v: Vec3) = Vec3(this.toFloat() / v.x, this.toFloat() / v.y, this.toFloat() / v.z)

data class IVec2(var x: Int, var y: Int) {
    constructor() : this(0)
    constructor(s: Int) : this(s, s)
    constructor(v: Vec2) : this(v.x.toInt(), v.y.toInt())
    constructor(v: Vec3) : this(v.x.toInt(), v.y.toInt())
}

data class BVec2(var x: Boolean, var y: Boolean) {
    constructor() : this(false)
    constructor(s: Boolean) : this(s, s)
    constructor(v: Vec2) : this(v.x != 0f, v.y != 0f)
    constructor(v: Vec3) : this(v.x != 0f, v.y != 0f)

    operator fun not() = BVec2(!x, !y)
}

// m12 -> mij, in column major, i is row and j is column, and values are filled and accessed (for vector access) along columns
// so this is, like, stupid maybe, but separate fields like this are not necessarily contiguous in memory, right? array instead?

//primary constructor is in column major
data class Mat2(
    var m00: Float, var m10: Float,
    var m01: Float, var m11: Float
) {
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

data class Mat3(
    var m00: Float, var m10: Float, var m20: Float,
    var m01: Float, var m11: Float, var m21: Float,
    var m02: Float, var m12: Float, var m22: Float
) {
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

    val transposed get() = Mat3(this).transpose()
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

data class Mat4(
    var m00: Float, var m10: Float, var m20: Float, var m30: Float,
    var m01: Float, var m11: Float, var m21: Float, var m31: Float,
    var m02: Float, var m12: Float, var m22: Float, var m32: Float,
    var m03: Float, var m13: Float, var m23: Float, var m33: Float
) {
    constructor(s: Number = 1) : this(s, s, s, s)
    constructor(m00: Number, m11: Number, m22: Number, m33: Number) : this(
        m00.toFloat(), 0f, 0f, 0f,
        0f, m11.toFloat(), 0f, 0f,
        0f, 0f, m22.toFloat(), 0f,
        0f, 0f, 0f, m33.toFloat()
    )

    constructor(diag: Vec4) : this(diag[0], diag[1], diag[2], diag[3])
    constructor(c0: Vec4, c1: Vec4, c2: Vec4, c3: Vec4) : this(
        c0[0], c0[1], c0[2], c0[3],
        c1[0], c1[1], c1[2], c1[3],
        c2[0], c2[1], c2[2], c2[3],
        c3[0], c3[1], c3[2], c3[3]
    )

    constructor(mat: Mat4) : this(
        mat.m00, mat.m10, mat.m20, mat.m30,
        mat.m01, mat.m11, mat.m21, mat.m31,
        mat.m02, mat.m12, mat.m22, mat.m32,
        mat.m03, mat.m13, mat.m23, mat.m33
    )

    val transposed get() = Mat4(this).transpose()
    fun transpose() = this(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33)

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

    operator fun invoke(s: Number) = this(s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, s)
    operator fun invoke(
        m00: Number, m10: Number, m20: Number, m30: Number,
        m01: Number, m11: Number, m21: Number, m31: Number,
        m02: Number, m12: Number, m22: Number, m32: Number,
        m03: Number, m13: Number, m23: Number, m33: Number
    ) {
        this.m00 = m00.toFloat(); this.m01 = m01.toFloat(); this.m02 = m02.toFloat(); this.m03 = m03.toFloat()
        this.m10 = m10.toFloat(); this.m11 = m11.toFloat(); this.m12 = m12.toFloat(); this.m13 = m13.toFloat()
        this.m20 = m20.toFloat(); this.m21 = m21.toFloat(); this.m22 = m22.toFloat(); this.m23 = m23.toFloat()
        this.m30 = m30.toFloat(); this.m31 = m31.toFloat(); this.m32 = m32.toFloat(); this.m33 = m33.toFloat()
    }
}
