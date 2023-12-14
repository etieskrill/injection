#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTexCoords;

out vec3 tNormal;
out vec2 tTexCoords;
out vec4 tFragPos;

uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

void main()
{
    tNormal = normalize(uNormal * iNormal);
    tTexCoords = iTexCoords;
    tFragPos = uModel * vec4(iPosition, 1.0);
    gl_Position = uCombined * uModel * vec4(iPosition, 1.0);
}
