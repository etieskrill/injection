#version 330 core

in vec4 iColour;

out vec4 oColour;

void main(void) {

    gl_Color = iColour;

}
