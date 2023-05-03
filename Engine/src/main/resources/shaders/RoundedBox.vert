#version 330 core

in vec2 position;

out vec4 colour;

void main(void) {

    gl_Position = vec4(position, 0.0, 1.0);
    colour = vec4(0.3, 0.3, 0.3, 1.0);

}
