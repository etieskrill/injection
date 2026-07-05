package org.etieskrill.engine.common

import kotlin.math.max
import kotlin.math.min

fun interface Interpolator {
    fun interpolate(t: Float): Float

    companion object {
        fun of(block: (Float) -> Float) = Interpolator { t -> min(max(t, 0f), 1f) }

        val LINEAR = of { t -> t }
        val QUADRATIC = of { t -> t * t }
        val INV_QUADRATIC = of { t -> 1 - ((t - 1) * (t - 1)) }
        val SMOOTHSTEP = of { t -> -2 * t * t * t + 3 * t * t }
    }
}
