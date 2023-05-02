#version 450 core

in vec2 position;

out vec3 colour;

void main(void) {

    gl_Position = vec4(position, 0, 1.0);
    colour = vec3(position.x + 0.5, position.y + 0.5, 1.0);

}