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
out vec4 dirLightSpaceFragPos;

uniform vec3 position;
uniform mat4 dirLightCombined;

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    for (int i = 0; i < 4; i++) {
        vec4 cornerPosition = vec4(position, 1.0);
        cornerPosition.xz += camera.far * corners[i] + camera.position.xz;
        texCoords = cornerPosition.xz;
        gl_Position = camera.combined * cornerPosition;
        dirLightSpaceFragPos = dirLightCombined * cornerPosition;
        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;
in vec4 dirLightSpaceFragPos;

out vec4 fragColour;

uniform sampler2D diffuse;
uniform sampler2DShadow dirShadowMap;

float getInShadow(vec4 lightSpaceFragPos);

void main() {
    fragColour = texture(diffuse, 0.1 * texCoords);
    fragColour.rgb *= 0.1 + getInShadow(dirLightSpaceFragPos);

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}

float getInShadow(vec4 lightSpaceFragPos) {
    vec3 screenSpace = lightSpaceFragPos.xyz / lightSpaceFragPos.w;
    vec3 depthSpace = screenSpace;
    depthSpace.xy = depthSpace.xy * 0.5 + 0.5;

    //    float bias = 0.005;
    //    depthSpace.z -= bias;
    depthSpace.z = 1.0; //FIXME

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
