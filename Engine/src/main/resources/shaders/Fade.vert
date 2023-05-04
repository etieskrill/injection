#version 330 core

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec4 ivColour;

out vec4 iColour;

//uniform vec4 uFadingColour;

void main(void) {

    gl_Position = vec4(iPosition, 1.0);
    iColour = ivColour;

}