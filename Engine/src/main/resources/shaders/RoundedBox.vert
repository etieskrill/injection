#version 330 core

in vec3 iPosition;
in vec4 iColour;

out vec4 oColour;

void main(void) {

    gl_Position = vec4(iPosition, 1.0);
    oColour = vec4(0.3, 0.3, 0.3, 1.0);

}
