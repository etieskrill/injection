#version 330 core

#ifdef VERTEX_SHADER
void main()
{}
#endif

#ifdef GEOMETRY_SHADER
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

out vec2 texCoords;

//uniform vec2 position;
//uniform vec2 size;

const vec2 corners[4] = vec2[](vec2(0, 0), vec2(1, 0), vec2(0, 1), vec2(1, 1));

void main()
{
    const vec2 size = vec2(0.33, 0.33 * 16 / 9);
    const vec2 position = vec2(1) - size;

    for (int i = 0; i < 4; i++) {
        gl_Position = vec4(position + size * corners[i], 0, 1);
        texCoords = corners[i];
        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;

uniform sampler2D tex;

void main()
{
    //    vec4 depth = texture(tex, texCoords);
    //    gl_FragColor = vec4(depth, depth, depth, 1.0);
    gl_FragColor = texture(tex, texCoords);
}
#endif
