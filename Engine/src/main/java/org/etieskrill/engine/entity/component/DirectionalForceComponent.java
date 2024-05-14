package org.etieskrill.engine.entity.component;

import org.joml.Vector3f;
import org.joml.Vector3fc;

//TODO add discriminator (string id or something) so classes can be reused - this wrapper type thing is a little stupid
public class DirectionalForceComponent {

    private final Vector3f force;

    public DirectionalForceComponent(final Vector3fc force) {
        this.force = new Vector3f(force);
    }

    public Vector3fc getForce() {
        return force;
    }

    public void setForce(Vector3fc force) {
        this.force.set(force);
    }

}
