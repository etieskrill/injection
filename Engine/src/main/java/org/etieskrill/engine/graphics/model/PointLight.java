package org.etieskrill.engine.graphics.model;

import glm_.vec3.Vec3;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.UniformMappable;

public class PointLight implements UniformMappable {
    
    private final Vec3 position;
    
    private final Vec3 ambient;
    private final Vec3 diffuse;
    private final Vec3 specular;
    
    private final float constant;
    private final float linear;
    private final float quadratic;
    
    public PointLight(Vec3 position, Vec3 ambient, Vec3 diffuse, Vec3 specular, float constant, float linear, float quadratic) {
        this.position = position;
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }
    
    @Override
    public boolean map(ShaderProgram.MapperShaderProgram shader) {
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