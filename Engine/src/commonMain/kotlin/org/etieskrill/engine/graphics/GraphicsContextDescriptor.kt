package org.etieskrill.engine.graphics

interface GraphicsContextDescriptor<T> {

    fun createForContext(context: GraphicsContext): T

}
