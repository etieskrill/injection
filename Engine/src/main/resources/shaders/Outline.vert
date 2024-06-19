#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;

uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

uniform float thicknessFactor;

void main() {
    vec3 tNormal = normalize(normal * iNormal);
    gl_Position = combined * model * vec4(iPosition + tNormal * thicknessFactor, 1.0);
}