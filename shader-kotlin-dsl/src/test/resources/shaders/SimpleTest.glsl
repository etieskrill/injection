#version 330 core

struct VertexData {
    vec4 position;
    vec2 texCoord;
};

uniform mat4 model;
uniform mat4 combined;

#pragma stage vertex

in vec3 position;
in vec2 texCoord;

out VertexData vertex;

void main() {
    vec4 position = combined * model * vec4(position, 1.0);
    gl_Position = position;
    vertex.texCoord = texCoord;
}

#pragma stage fragment

in VertexData vertex;

out vec4 colour;
out vec4 bloom;

void main() {
    colour = gl_Position;
    bloom = vec4(0.0, 0.0, 0.0, 1.0);
}

