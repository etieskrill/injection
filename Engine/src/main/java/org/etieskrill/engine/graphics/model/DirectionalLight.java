package org.etieskrill.engine.graphics.model;

import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.UniformMappable;

public class DirectionalLight implements UniformMappable {
    
    private final Vec3 direction;
    
    private final Vec3 ambient;
    private final Vec3 diffuse;
    private final Vec3 specular;
    
    public DirectionalLight(Vec3 direction, Vec3 ambient, Vec3 diffuse, Vec3 specular) {
        this.direction = direction;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
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