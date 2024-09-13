#version 330 core

#extension GL_ARB_shading_language_include: require

#include "/include/InjectionCore.glsl"

uniform Camera camera, dirLightCamera;

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
out vec4 dirLightSpaceFragPos;

uniform vec3 position;

uniform mat4 dirLightPerspective;
uniform mat4 dirLightCombined;

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    vec4 quadOffset = camera.perspective * vec4(billBoard.size, 1, 1);
    quadOffset.xyz /= quadOffset.w;
    quadOffset.x = -quadOffset.x;

    vec4 pos = camera.combined * vec4(position + billBoard.offset, 1);
    pos.y -= quadOffset.y;

    vec4 lightSpacePos = vec4(position + billBoard.offset, 1);
    lightSpacePos.y -= billBoard.size.y;

    for (int i = 0; i < 4; i++) {
        vec4 cornerOffset = vec4(quadOffset.xy * corners[i], 0, 0);
        gl_Position = pos + cornerOffset;
        texCoords = (corners[i] / 2.0) + 0.5;

        vec4 dirLightCornerOffset = vec4(billBoard.size * corners[i], 0, 0);
        dirLightSpaceFragPos = dirLightCamera.combined * (lightSpacePos + dirLightCornerOffset);

        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;
in vec4 dirLightSpaceFragPos;

out vec4 fragColour;

uniform DirectionalLight dirLight;

uniform sampler2DShadow dirShadowMap;

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection);

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

    fragColour.rgb *= 0.1 + getInShadow(dirLightSpaceFragPos, dirLight.direction);

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection) {
    vec3 screenSpace = lightSpaceFragPos.xyz / lightSpaceFragPos.w;
    vec3 depthSpace = screenSpace * 0.5 + 0.5;

    //    if (depthSpace.z > 1.0) return 1.0;

    //    float bias = min(0.005, 0.05 * (1.0 - dot(vert_out.normal, lightDirection)));
    float bias = 0.005;
    depthSpace.z -= bias;
    float shadow = 0.0;
    //    depthSpace.z = 1 - depthSpace.z;
    //    depthSpace.z = 1.0;

    //    vec3 texelSize = vec3(1.0 / textureSize(dirShadowMap, 0), 1.0);
    //    for (int x = -1; x <= 1; x++) {
    //        for (int y = -1; y <= 1; y++) {
    //            FIXME on nvidia this texture call produces a warning for invalid textures, even if it is never called for
    //            FIXME said invalid state - so the one material to one shader paradigm seems to be almost enforced
    //            i know this because if it were called, the entire pipeline for the mesh would fail, and not produce any visible result
    //            shadow += texture(dirShadowMap, depthSpace + vec3(x, y, 0.0) * texelSize);
    //        }
    //    }
    shadow += texture(dirShadowMap, depthSpace);

    return shadow;
    //    return shadow / 9.0;
}
#endif
