package org.etieskrill.engine.graphics.framebuffer

class FrameBufferCreationException(
    override val message: String,
    override val cause: Throwable? = null,
    infoLog: String? = null
) : RuntimeException("$message\n\n$infoLog", cause)
