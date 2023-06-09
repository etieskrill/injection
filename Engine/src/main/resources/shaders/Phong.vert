#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec4 iColour;
layout (location = 3) in vec2 iTextureCoords;

out vec3 tNormal;
out vec3 tColour;
out vec2 tTextureCoords;
out vec3 tFragPos;

uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uView;
uniform mat4 uProjection;

void main() {

    gl_Position = uProjection * uView * uModel * vec4(iPosition, 1.0);
    tNormal = normalize(uNormal * iNormal);
    tColour = vec3(iColour);
    tTextureCoords = iTextureCoords;
    tFragPos = vec3(uModel * vec4(iPosition, 1.0));

}