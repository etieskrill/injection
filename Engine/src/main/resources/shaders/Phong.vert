#version 410 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTextureCoords;

out vec3 tNormal;
out vec2 tTextureCoords;
out vec3 tFragPos;

uniform mat4 uMesh;
uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

uniform vec2 uTextureScale;

void main()
{
    tNormal = normalize(uNormal * iNormal);
    tTextureCoords = iTextureCoords * uTextureScale;
    tFragPos = vec3(uModel * uMesh * vec4(iPosition, 1.0));
    gl_Position = uCombined * uModel * uMesh * vec4(iPosition, 1.0);
}