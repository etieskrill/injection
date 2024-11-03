#version 330 core

#ifdef VERTEX_SHADER
void main()
{}
#endif

#ifdef GEOMETRY_SHADER
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

out vec2 offset;

uniform vec4 position;
uniform float radius;
uniform float aspect;

const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    for (int i = 0; i < 4; i++) {
        offset = vertices[i] * radius;
        gl_Position = vec4(position.x + offset.x, position.y + offset.y * aspect, 0, 1);
        EmitVertex();
    }
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 offset;

out vec4 fragColour;

uniform float radius;

void main()
{
    if (length(offset) <= radius) {
        fragColour = vec4(1, 0, 0, 0.25);
    } else {
        discard;
    }
}
#endif
