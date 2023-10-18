#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 2) in vec2 iTextureCoords;

out vec2 tTextureCoords;

uniform mat4 uModel;
uniform mat4 uProjection;
uniform mat4 uCombined;

void main()
{
    tTextureCoords = iTextureCoords;
    gl_Position = vec4((uCombined * uModel * vec4(iPosition.xyz, 1.0)).xy, 0.0, 1.0);
}
