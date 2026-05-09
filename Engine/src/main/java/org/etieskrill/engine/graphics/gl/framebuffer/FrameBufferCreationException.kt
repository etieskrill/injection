package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.graphics.gl.exception.GLError
import org.etieskrill.engine.graphics.gl.exception.GLException


class FrameBufferCreationException(
    override val message: String,
    override val cause: Throwable? = null,
    override val infoLog: String? = null,
    override val errorCode: GLError? = null
) : GLException(message, cause, infoLog, errorCode)
