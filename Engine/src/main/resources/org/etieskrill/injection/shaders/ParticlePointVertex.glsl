#version 410 core

uniform struct Camera {
    mat4 view;
    mat4 perspective;
    mat4 combined;
} camera;

#ifdef VERTEX_SHADER
layout (location = 0) attribute vec3 a_Position;
layout (location = 1) attribute vec2 a_Transform0;
layout (location = 2) attribute vec2 a_Transform1;
layout (location = 3) attribute vec4 a_Colour;

flat out vec4 particleColour;
flat out mat2 transform;

uniform mat4 model;

void main()
{
    gl_Position = camera.combined * model * vec4(a_Position, 1.0);
    particleColour = a_Colour;
    transform = mat2(a_Transform0, a_Transform1);
}
#endif

#ifdef GEOMETRY_SHADER
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

flat in vec4 particleColour[];
flat in mat2 transform[];

out vec2 texCoords;
flat out vec4 colour;

uniform float size;

const vec2 vertices[4] = vec2[](vec2(-0.5, -0.5), vec2(0.5, -0.5), vec2(-0.5, 0.5), vec2(0.5, 0.5));

void main()
{
    for (int i = 0; i < 4; i++) {
        vec2 offset = (camera.perspective * vec4(vertices[i], 0, 0)).xy;
        gl_Position = gl_in[0].gl_Position + vec4(offset * size, -0.05, 0);
        texCoords = (transform[0] * (vertices[i])) + vec2(0.5, 0.5);
        colour = particleColour[0];
        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;
flat in vec4 colour;

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

uniform sampler2D sprite;

void main()
{
    fragColour = texture(sprite, texCoords) * colour;
    if (fragColour.a == 0.0) discard;

    float brightness = dot(fragColour.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = fragColour;
}
#endif
