package org.etieskrill.engine.entity.component

import net.bifreus.kml.Vec3

//TODO add discriminator (string id or something) so classes can be reused - this wrapper type thing is a little stupid
open class DirectionalForceComponent(
    force: Vec3
) {
    var force: Vec3 = force
        set(value) {
            field(value)
        }
}
