package org.etieskrill.engine.entity.component

import org.joml.Vector3f

class Acceleration(force: Vector3f, var factor: Float = 1f) : DirectionalForceComponent(force)
