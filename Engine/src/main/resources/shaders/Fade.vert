#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec4 iColour;
layout (location = 2) in vec2 iTextureCoords;

out vec4 tColour;
out vec2 tTextureCoords;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

void main() {

    gl_Position = uProjection * uView * uModel * vec4(iPosition, 1.0);
    tColour = iColour;
    tTextureCoords = iTextureCoords;

}