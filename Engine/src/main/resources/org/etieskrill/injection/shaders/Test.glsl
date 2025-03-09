#version 330 core

struct VertexData {
    vec4 position;
    vec2 texCoord;
};

uniform mat4 model;
uniform mat4 combined;

#pragma stage vertex

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;

out VertexData vertex;

void main() {
    vertex.position = combined * model * vec4(position, 1.0);
    vertex.texCoord = texCoord;

    mat4 mat_0 = combined * model;
}

#pragma stage fragment

in VertexData vertex;

out vec4 colour;
out vec4 bloom;

void main() {
    colour = vertex.position;
    bloom = vec4(0.0, 0.0, 0.0, 1.0);
}
