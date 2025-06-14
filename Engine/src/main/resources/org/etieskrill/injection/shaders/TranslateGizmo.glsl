#version 330 core

#pragma stage vert

in vec3 a_Position;

uniform vec3 position;
uniform mat4 model;
uniform mat4 combined;

void main() {
    gl_Position = combined * model * vec4(position + 100 * a_Position, 1.0);
}

#pragma stage frag

out vec4 fragColour;

uniform vec4 colour;

void main() {
    fragColour = vec4(colour);
}
