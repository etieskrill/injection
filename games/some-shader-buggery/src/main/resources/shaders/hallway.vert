#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTexCoords;

out vec3 tNormal;
out vec2 tTexCoords;
out vec4 tFragPos;

uniform mat4[80] uModels;
uniform mat3[80] uNormals;
uniform mat4 combined;

void main()
{
    tNormal = normalize(uNormals[gl_InstanceID] * iNormal);
    tTexCoords = iTexCoords;
    tFragPos = uModels[gl_InstanceID] * vec4(iPosition, 1.0);
    gl_Position = combined * uModels[gl_InstanceID] * vec4(iPosition, 1.0);
}
