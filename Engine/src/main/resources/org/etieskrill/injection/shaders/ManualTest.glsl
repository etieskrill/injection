#version 330 core

# dis some manual shit yo

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
    vertex.position = position;
    vertex.texCoord = texCoord;
}

#pragma stage fragment

in VertexData vertex;

out vec4 colour;
out vec4 bloom;

void main() {
    colour = vertex.position;
    bloom = vec4(0.0, 0.0, 0.0, 1.0);
}
