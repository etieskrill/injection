#version 330 core

layout (points) in;
layout (line_strip, max_vertices = 2) out;

in vec3 t_Normal[1];

uniform mat4 uMesh;
uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

void main() {
    mat4 transform = uCombined * uModel * uMesh;

    gl_Position = transform * gl_in[0].gl_Position;
    EmitVertex();

    gl_Position = transform * gl_in[0].gl_Position + normalize(uCombined * vec4(uNormal * t_Normal[0], 0.0));
    EmitVertex();
}