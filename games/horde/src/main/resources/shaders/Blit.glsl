#version 330 core

uniform struct Camera {
    mat4 perspective;
    mat4 combined;
    vec3 position;
    float far;
    ivec2 viewport;
    float aspect;
} camera;

#ifdef VERTEX_SHADER
void main()
{}
#endif

#ifdef GEOMETRY_SHADER
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

out vec2 texCoords;

uniform vec3 position;
uniform vec3 offset;
uniform vec2 size;

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    vec4 quadOffset = camera.perspective * vec4(size, 1, 1);
    quadOffset.xyz /= quadOffset.w;

    vec4 pos = camera.combined * vec4(position + offset, 1);
    pos.y -= quadOffset.y;

    for (int i = 0; i < 4; i++) {
        vec4 cornerPos = pos + vec4(quadOffset.xy * corners[i], 0, 0);
        texCoords = (corners[i] / 2.0) + 0.5;
        gl_Position = cornerPos;
        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;

out vec4 fragColour;

uniform sampler2D diffuse;

void main() {
    fragColour = texture(diffuse, texCoords);
    if (fragColour.a == 0) discard;

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}
#endif
