package org.etieskrill.engine.graphics

import org.etieskrill.engine.graphics.framebuffer.FrameBuffer

expect class GraphicsContext {

    var screenBuffer: FrameBuffer internal set

    fun <T> withContext(block: () -> T): T

}

// scratch file
//
////multiple windows with separate contexts
//
//val window1 = Window()
//val context1 = window1.context
//val window2 = Window()
//val context2 = window2.context
//
////option 1 - always pass context via parameter in constructor
//val frameBuffer1 = FrameBuffer(context = context1)
//val frameBuffer2 = FrameBuffer(context = context2)
//
////option 2 - factory wrapper around above constructor
//val frameBuffer1 = context1.createFrameBuffer()
//val frameBuffer2 = context2.createFrameBuffer()
//
////option 3 - have a variable in coroutine context (not much better than option 2 looking at this)
//// these constructors could just be methods in the context instead
//// my primary interest is that creation without context should not even be possible and should not compile
//val frameBuffer1 = context1.withContext { FrameBuffer() } //check if any context active
//val frameBuffer2 = context2.withContext { FrameBuffer() }
//
////multiple windows with shared context
//
//val window1 = Window()
//val context = window1.context
//val window2 = Window(context = context)
//
//val frameBuffer = context.createFrameBuffer()
