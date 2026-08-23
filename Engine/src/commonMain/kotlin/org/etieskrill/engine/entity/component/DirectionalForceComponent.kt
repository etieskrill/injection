package org.etieskrill.engine.entity.component

import org.joml.Vector3f

//TODO add discriminator (string id or something) so classes can be reused - this wrapper type thing is a little stupid
open class DirectionalForceComponent(
    force: Vector3f
) {
    var force: Vector3f = force
        set(value) {
            field.set(value)
        }
}
