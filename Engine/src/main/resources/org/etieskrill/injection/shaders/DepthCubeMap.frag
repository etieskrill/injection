#version 330 core

in vec4 fragPos;

uniform struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
} light;

uniform float farPlane;
uniform float time;

void main()
{
    float distance = length(fragPos.xyz - light.position);
    distance = distance / farPlane;
    gl_FragDepth = distance;
}