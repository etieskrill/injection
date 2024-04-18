#version 330 core

#define NUM_SIDES 6

layout(triangles) in;
layout(triangle_strip, max_vertices = 18) out;

out vec4 fragPos;

uniform mat4 shadowCombined[NUM_SIDES];
uniform int index;

void main()
{
    for (int face = 0; face < NUM_SIDES; face++)
    {
        gl_Layer = 6 * index + face;
        for (int i = 0; i < 3; i++)
        {
            fragPos = gl_in[i].gl_Position;
            gl_Position = shadowCombined[face] * fragPos;
            EmitVertex();
        }
        EndPrimitive();
    }
}
