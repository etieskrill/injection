#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTextureCoords;

out vec2 tTextureCoords;

uniform mat4 uModel;
uniform mat4 uCombined;

void main()
{
    tTextureCoords = iTextureCoords;
    gl_Position = uCombined * uModel * vec4(iPosition, 1.0);
}
