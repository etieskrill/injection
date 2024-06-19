#version 330 core

layout (points) in;
layout (line_strip, max_vertices = 6) out;

in Data {
    vec3 normal;
    vec3 tangent;
    vec3 biTangent;
} vert_out[1];

out vec4 lineColour;

uniform mat4 mesh;
uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

void main() {
    vec4 position = combined * model * mesh * gl_in[0].gl_Position;
    float length = 1.0;

    gl_Position = position;
    lineColour = vec4(1.0, 0.0, 0.0, 1.0);
    EmitVertex();
    gl_Position = position + length * normalize(combined * vec4(normal * vert_out[0].normal, 0.0));
    lineColour = vec4(1.0, 0.0, 0.0, 1.0);
    EmitVertex();
    EndPrimitive();

    gl_Position = position;
    lineColour = vec4(0.0, 1.0, 0.0, 1.0);
    EmitVertex();
    gl_Position = position + length * normalize(combined * vec4(normal * vert_out[0].tangent, 0.0));
    lineColour = vec4(0.0, 1.0, 0.0, 1.0);
    EmitVertex();
    EndPrimitive();

    gl_Position = position;
    lineColour = vec4(0.0, 0.0, 1.0, 1.0);
    EmitVertex();
    gl_Position = position + length * normalize(combined * vec4(normal * vert_out[0].biTangent, 0.0));
    lineColour = vec4(0.0, 0.0, 1.0, 1.0);
    EmitVertex();
    EndPrimitive();
}