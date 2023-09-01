#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;

uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

uniform float uThicknessFactor;

void main() {
    vec3 tNormal = normalize(uNormal * iNormal);
    gl_Position = uCombined * uModel * vec4(iPosition + tNormal * uThicknessFactor, 1.0);
}