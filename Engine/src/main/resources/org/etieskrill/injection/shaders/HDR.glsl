#version 330 core

#ifdef VERTEX_SHADER
const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

varying vec2 texCoords;

void main()
{
    gl_Position = vec4(vertices[gl_VertexID], 0, 1);
    texCoords = max(vertices[gl_VertexID], vec2(0, 0));
}
#endif

#ifdef FRAGMENT_SHADER
varying vec2 texCoords;

out vec4 fragColour;

uniform sampler2D hdrBuffer;
uniform float exposure;
uniform bool reinhard;

uniform sampler2D bloomBuffer;

void main()
{
    vec3 hdr = texture(hdrBuffer, texCoords).rgb;
    vec3 bloom = texture(bloomBuffer, texCoords).rgb;
    hdr += bloom;

    vec3 mapped;
    if (reinhard) {
        mapped = hdr / (hdr + vec3(1.0));
    } else {
        mapped = vec3(1.0) - exp(-hdr * exposure);
    }

    fragColour = vec4(pow(mapped, vec3(1.0 / 2.2)), 1.0);
}
#endif
