#version 330 core

uniform struct Camera {
    mat4 perspective;
    mat4 combined;
    vec3 position;
    float near;
    float far;
    ivec2 viewport;
    float aspect;
} camera, dirLightCamera;

uniform struct BillBoard {
    sampler2D sprite;
    vec2 size;
    vec3 offset;
    float rotation;
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

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    vec4 quadOffset = camera.perspective * vec4(billBoard.size, 1, 1);
    quadOffset.xyz /= quadOffset.w;
    quadOffset.x = -quadOffset.x;

    vec4 pos = camera.combined * vec4(position + billBoard.offset, 1);
    pos.y -= quadOffset.y;

    vec3 lightSpacePos = position - billBoard.offset;
    lightSpacePos.y -= billBoard.size.y;

    float rotSin = sin(-billBoard.rotation);
    float rotCos = cos(-billBoard.rotation);
    mat2 rotation = mat2(rotCos, rotSin, -rotSin, rotCos);

    for (int i = 0; i < 4; i++) {
        vec4 cornerOffset = camera.perspective * vec4(rotation * (billBoard.size * corners[i]), 1, 1);
        cornerOffset.xyz /= cornerOffset.w;
        cornerOffset.x = -cornerOffset.x;
        cornerOffset.zw = vec2(0);

        gl_Position = pos + cornerOffset;

        texCoords = (corners[i] / 2.0) + 0.5;

        vec2 lightCornerOffset = billBoard.size.xy * corners[i];
        lightCornerOffset.x = -lightCornerOffset.x;
        dirLightSpaceFragPos = dirLightCamera.combined * vec4(lightSpacePos + vec3(lightCornerOffset, 0), 1);

        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;
in vec4 dirLightSpaceFragPos;

out vec4 fragColour;

uniform struct DirectionalLight {
    vec3 direction;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
} dirLight;

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

    if (depthSpace.z > 1.0) return 1.0;

    //    float bias = min(0.005, 0.05 * (1.0 - dot(vert_out.normal, lightDirection)));
    float bias = 0.005;
    depthSpace.z -= bias;
    float shadow = 0.0;

    vec3 texelSize = vec3(1.0 / textureSize(dirShadowMap, 0), 1.0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            shadow += texture(dirShadowMap, depthSpace + vec3(x, y, 0.0) * texelSize);
        }
    }

    return shadow / 9.0;
}
#endif
