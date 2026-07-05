#version 410 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTextureCoords;

out vec3 tNormal;
out vec2 tTextureCoords;
out vec3 tFragPos;

uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

void main() {

    gl_Position = combined * model * vec4(iPosition, 1.0);
    tNormal = normalize(normal * iNormal);
    tTextureCoords = iTextureCoords;
    tFragPos = vec3(model * vec4(iPosition, 1.0));

}