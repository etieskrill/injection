#version 330 core

uniform struct Camera {
    mat4 combined;
    vec3 position;
    float far;
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

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    for (int i = 0; i < 4; i++) {
        vec4 cornerPosition = vec4(position, 1.0);
        cornerPosition.xz += camera.far * corners[i] + camera.position.xz;
        texCoords = cornerPosition.xz;
        gl_Position = camera.combined * cornerPosition;
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
    fragColour = texture(diffuse, 0.1 * texCoords);

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}
#endif
