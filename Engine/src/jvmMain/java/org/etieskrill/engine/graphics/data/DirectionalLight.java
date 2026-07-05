package org.etieskrill.engine.graphics.data;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DirectionalLight implements UniformMappable {

    private final Vector3f direction;

    private final Vector3f ambient;
    private final Vector3f diffuse;
    private final Vector3f specular;

    public DirectionalLight(Vector3f direction) {
        this(direction, new Vector3f(1), new Vector3f(1), new Vector3f(1));
    }

    public DirectionalLight(Vector3f direction, Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.direction = direction;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        this.direction.set(direction);
    }

    public Vector3f getAmbient() {
        return ambient;
    }

    public void setAmbient(Vector3fc ambient) {
        this.ambient.set(ambient);
    }

    public Vector3f getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Vector3fc diffuse) {
        this.diffuse.set(diffuse);
    }

    public Vector3f getSpecular() {
        return specular;
    }

    public void setSpecular(Vector3fc specular) {
        this.specular.set(specular);
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper mapper) {
        mapper
                .map("direction", direction)
                .map("ambient", ambient)
                .map("diffuse", diffuse)
                .map("specular", specular);
        return true;
    }

}
