package org.etieskrill.engine.buffer

import org.joml.Matrix2fc
import org.joml.Vector2fc
import org.joml.Vector3fc
import org.joml.Vector4fc
import org.joml.Vector4ic
import org.lwjgl.BufferUtils
import io.github.etieskrill.injection.extension.shader.ByteBuffer as DslByteBuffer

actual class ByteBuffer actual constructor(val size: Long) : DslByteBuffer {

    internal val buffer = BufferUtils.createByteBuffer(size.toInt())

    actual var writeHead: Long = 0L
        set(value) {
            check(value <= size) { "Write head must be in the range 0..<size, was $value" }
            field = value
        }

    actual var readHead: Long = 0L
        set(value) {
            check(value <= writeHead) { "Read head must be in the range 0..writeHead, was $value" }
            field = value
        }

    actual var remaining: Long = writeHead - readHead

    actual fun clear() {
        writeHead = 0L
        readHead = 0L
        buffer.clear()
    }

    actual fun put(buffer: ByteBuffer, offset: Long) {
        writeHead += buffer.remaining
        this.buffer.put(buffer.buffer)
    }

    actual operator fun plusAssign(i: Int?) {
        i?.let { buffer.putInt(it) } ?: buffer.putInt(0)
        writeHead += Int.SIZE_BYTES
    }

    actual operator fun plusAssign(f: Float?) {
        f?.let { buffer.putFloat(it) } ?: buffer.putFloat(0.0f)
        writeHead += Float.SIZE_BYTES
    }

    actual operator fun plusAssign(v: Vector2fc?) {
        v?.let { this.buffer.putFloat(it.x()).putFloat(it.y()) }
            ?: this.buffer.putFloat(0f).putFloat(0f)
        writeHead += 2 * Float.SIZE_BYTES
    }

    actual operator fun plusAssign(v: Vector3fc?) {
        v?.let { this.buffer.putFloat(it.x()).putFloat(it.y()).putFloat(it.z()) }
            ?: this.buffer.putFloat(0f).putFloat(0f).putFloat(0f)
        writeHead += 3 * Float.SIZE_BYTES
    }

    actual operator fun plusAssign(v: Vector4fc?) {
        v?.let { this.buffer.putFloat(it.x()).putFloat(it.y()).putFloat(it.z()).putFloat(it.w()) }
            ?: this.buffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f)
        writeHead += 4 * Float.SIZE_BYTES
    }

    actual operator fun plusAssign(v: Vector4ic?) {
        v?.let { this.buffer.putInt(it.x()).putInt(it.y()).putInt(it.z()).putInt(it.w()) }
            ?: this.buffer.putInt(0).putInt(0).putInt(0).putInt(0)
        writeHead += 4 * Int.SIZE_BYTES
    }

    actual operator fun plusAssign(m: Matrix2fc?) {
        m?.let { this.buffer.putFloat(it.m00()).putFloat(it.m01()).putFloat(it.m10()).putFloat(it.m11()) }
            ?: this.buffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f)
        writeHead += 4 * Float.SIZE_BYTES
    }

}
