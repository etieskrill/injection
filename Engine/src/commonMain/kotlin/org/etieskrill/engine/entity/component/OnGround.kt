package org.etieskrill.engine.entity.component

data class OnGround(
    val jumpStrength: Float,
    val bumpStrength: Float,
    var isOnGround: Boolean = true
)
