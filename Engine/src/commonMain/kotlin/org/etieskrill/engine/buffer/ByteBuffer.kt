package org.etieskrill.engine.buffer

import net.bifreus.kml.Mat2c
import net.bifreus.kml.Vec2c
import net.bifreus.kml.Vec3c
import net.bifreus.kml.Vec4c
import net.bifreus.kml.Vec4ic
import io.github.etieskrill.injection.extension.shader.ByteBuffer as DslByteBuffer

/**
 * A wrapper for data that will be passed to the GPU at some point.
 *
 * Has a fixed byte capacity, and a read/write head at whose position data will be put into and read out of the buffer.
 * The head is incremented by the size of the data type written or read respectively, and can be moved by a certain
 * number of bytes relative to its current position, absolute position, or reset to zero.
 *
 * Each setter (using the `+=` operator) is nullable, and the bytes at the respective position will be set to zero.
 */
expect class ByteBuffer : DslByteBuffer {

    constructor(size: Long)

    /**
     * The write head's position. Must be between zero and the size.
     */
    var writeHead: Long

    /**
     * The read head's position. Must be between zero and the write head.
     */
    var readHead: Long

    /**
     * The number of bytes left for reading in the buffer. Equal to [writeHead] - [readHead].
     */
    var remaining: Long

    /**
     * Sets both head positions to zero. Do this before writing new data to the buffer.
     */
    fun clear()

    /**
     * Sets the contents of this buffer at [offset] to the contents of [buffer].
     */
    fun put(buffer: ByteBuffer, offset: Long = 0L)

    operator fun plusAssign(i: Int?)
    operator fun plusAssign(f: Float?)
    operator fun plusAssign(v: Vec2c?)
    operator fun plusAssign(v: Vec3c?)
    operator fun plusAssign(v: Vec4c?)
    operator fun plusAssign(v: Vec4ic?)
    operator fun plusAssign(m: Mat2c?)

}
