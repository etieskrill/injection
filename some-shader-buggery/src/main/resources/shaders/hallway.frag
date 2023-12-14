#version 330 core

in vec3 tNormal;
in vec2 tTexCoords;
in vec4 tFragPos;

out vec4 oColour;

struct Material {
    vec4 colour;
    vec4 diffuseColour;
    vec4 emissiveColour;
    float emissiveIntensity;
    float opacity;
};

struct DirectionalLight {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform float uTime;

uniform Material material;

uniform DirectionalLight sun;

void main()
{
    vec3 sunDir = -sun.direction;
    float diff = max(dot(tNormal, sunDir), 0.0);
    vec4 diffuse = diff * material.diffuseColour;

    oColour = material.colour + diffuse + material.emissiveColour * material.emissiveIntensity;
    oColour *= material.opacity;
    oColour.rgb = pow(oColour.rgb, vec3(1 / 2.2));
}
