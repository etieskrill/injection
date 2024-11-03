#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 2) in vec2 iTextureCoords;

out vec2 tTextureCoords;

void main()
{
    tTextureCoords = iTextureCoords;
    gl_Position = vec4(iPosition.x, iPosition.y, 0.0, 1.0);
}
