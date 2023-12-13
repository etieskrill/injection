#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;

out vec3 tNormal;

uniform mat3 uNormal;

void main() {

    gl_Position = vec4(iPosition, 1.0);
    tNormal = normalize(uNormal * iNormal);

}
