package org.etieskrill.engine.graphics.data;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shader.UniformMappable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class PointLight implements UniformMappable {
    
    private final Vector3f position;
    
    private final Vector3f ambient;
    private final Vector3f diffuse;
    private final Vector3f specular;
    
    private final float constant;
    private final float linear;
    private final float quadratic;
    
    public PointLight(Vector3f position, Vector3f ambient, Vector3f diffuse, Vector3f specular, float constant, float linear, float quadratic) {
        this.position = position;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3fc position) {
        this.position.set(position);
    }

    @Override
    public boolean map(ShaderProgram.UniformMapper shader) {
        shader
                .map("position", position)
                .map("ambient", ambient)
                .map("diffuse", diffuse)
                .map("specular", specular)
                .map("constant", constant)
                .map("linear", linear)
                .map("quadratic", quadratic);
        return true;
    }
    
}
