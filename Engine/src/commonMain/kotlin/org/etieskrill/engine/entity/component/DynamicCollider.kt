package org.etieskrill.engine.entity.component

import net.bifreus.kml.Vec3

class DynamicCollider(
    var previousPosition: Vec3 = Vec3(), //TODO "inline" value -> object owns member, force set by value | reasons for/against this?
    var isStaticOnly: Boolean = false
)
