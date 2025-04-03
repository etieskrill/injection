#version 330 core

struct Vertex {
    vec2 texCoords;
};

const int someConst = 5;
const vec2 anotherConst = vec2(-1, 1);
const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

uniform sampler2D hdrBuffer;
uniform float exposure;
uniform bool reinhard;
uniform sampler2D bloomBuffer;

#pragma stage vertex

out Vertex vertex;

void main() {
    gl_Position = vec4(vertices[gl_VertexID], 0, 1);
    vertex.texCoords = max(vertices[gl_VertexID], vec2(0, 0));
}

#pragma stage fragment

in Vertex vertex;

out vec4 fragColour;

void main() {
    vec3 hdr = texture(hdrBuffer, vertex.texCoords).rgb;
    vec3 bloom = texture(bloomBuffer, vertex.texCoords).rgb;
    hdr = bloom;
    hdr = hdr + bloom;
    hdr += bloom;

    vec3 mapped;
    if (reinhard) {
        mapped = hdr / (hdr + vec3(1.0));
    } else {
        mapped = vec3(1.0) - exp(-hdr * exposure);
    }

    fragColour = vec4(pow(mapped, vec3(1.0 / 2.2)), 1.0);
}
