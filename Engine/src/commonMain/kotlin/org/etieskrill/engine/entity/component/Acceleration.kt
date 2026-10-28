package org.etieskrill.engine.entity.component

import net.bifreus.kml.Vec3

class Acceleration(force: Vec3, var factor: Float = 1f) : DirectionalForceComponent(force)
