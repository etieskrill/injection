package org.etieskrill.engine.entity.component

import org.joml.Vector3f

class DynamicCollider(
    var previousPosition: Vector3f = Vector3f(), //TODO "inline" value -> object owns member, force set by value | reasons for/against this?
    var isStaticOnly: Boolean = false
)
