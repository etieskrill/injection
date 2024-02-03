package org.etieskrill.engine.graphics.model;

import org.joml.Vector3f;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.UniformMappable;

public class DirectionalLight implements UniformMappable {
    
    private final Vector3f direction;
    
    private final Vector3f ambient;
    private final Vector3f diffuse;
    private final Vector3f specular;
    
    public DirectionalLight(Vector3f direction, Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.direction = direction;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
    }

    public void setDirection(Vector3f direction) {
        this.direction.set(direction);
    }
    
    @Override
    public boolean map(ShaderProgram.MapperShaderProgram shader) {
        shader
                .map("direction", direction)
                .map("ambient", ambient)
                .map("diffuse", diffuse)
                .map("specular", specular);
        return true;
    }
    
}
