package org.etieskrill.engine.graphics.framebuffer

import org.etieskrill.engine.graphics.framebuffer.FrameBufferAttachmentType.*
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT2
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT3
import org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT31
import org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL_ATTACHMENT
import org.lwjgl.opengl.GL30C.GL_STENCIL_ATTACHMENT

internal val FrameBufferAttachmentType.gl: Int
    get() = when (this) {
        COLOUR0 -> GL_COLOR_ATTACHMENT0
        COLOUR1 -> GL_COLOR_ATTACHMENT1
        COLOUR2 -> GL_COLOR_ATTACHMENT2
        COLOUR3 -> GL_COLOR_ATTACHMENT3
        COLOUR31 -> GL_COLOR_ATTACHMENT31
        DEPTH -> GL_DEPTH_ATTACHMENT
        STENCIL -> GL_STENCIL_ATTACHMENT
        DEPTH_STENCIL -> GL_DEPTH_STENCIL_ATTACHMENT
    }
