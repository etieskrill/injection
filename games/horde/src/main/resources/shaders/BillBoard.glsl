#version 330 core

uniform struct Camera {
    mat4 perspective;
    mat4 combined;
    vec3 position;
    float near;
    float far;
    ivec2 viewport;
    float aspect;
} camera;

uniform struct BillBoard {
    sampler2D sprite;
    vec2 size;
    vec3 offset;
    bool punchThrough;
} billBoard;

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
    vec4 quadOffset = camera.perspective * vec4(billBoard.size, 1, 1);
    quadOffset.xyz /= quadOffset.w;
    quadOffset.x = -quadOffset.x;

    vec4 pos = camera.combined * vec4(position + billBoard.offset, 1);
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

float toNonlinearDepth(const float linearDepth) {
    return (1 / linearDepth - 1 / camera.near) / (1 / camera.far - 1 / camera.near);
}

void main() {
    fragColour = texture(billBoard.sprite, texCoords);
    if (fragColour.a == 0) discard;

    float nearFadeStart = toNonlinearDepth(1.0);
    float nearFadeEnd = toNonlinearDepth(0.5);

    if (gl_FragCoord.z < nearFadeStart) {
        fragColour.a *= smoothstep(1.0, 0.0, (gl_FragCoord.z - nearFadeStart) / (nearFadeEnd - nearFadeStart));
    }

    if (billBoard.punchThrough) {
        float punchHoleStart = toNonlinearDepth(2.2);
        const float punchHoleBlurRadiusStart = 150;
        const float punchHoleBlurRadiusEnd = 200;

        if (gl_FragCoord.z < punchHoleStart) {
            float centerDistance = length(gl_FragCoord.xy - camera.viewport / 2);
            if (centerDistance > punchHoleBlurRadiusStart && centerDistance < punchHoleBlurRadiusEnd) {
                fragColour.a *= smoothstep(0.0, 1.0, (centerDistance - punchHoleBlurRadiusStart) / (punchHoleBlurRadiusEnd - punchHoleBlurRadiusStart));
            } else if (centerDistance < punchHoleBlurRadiusStart) {
                discard;
            }
        }
    }

    if (fragColour.a == 0) discard;

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}
#endif
