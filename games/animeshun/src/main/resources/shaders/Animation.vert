#version 410 core

#define MAX_BONES 100
#define MAX_BONE_INFLUENCES 4

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTextureCoords;
layout (location = 5) in ivec4 boneIds;
layout (location = 6) in vec4 weights;

out vec3 tNormal;
out vec2 tTextureCoords;
out vec3 tFragPos;

flat out ivec4 tBoneIds;
flat out vec4 tWeights;

uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

uniform mat4 boneMatrices[MAX_BONES];

void main()
{
    vec4 bonedPosition = vec4(0.0);
    vec3 bonedNormal = vec3(0.0);

    int bones = 0;
    for (int i = 0; i < MAX_BONE_INFLUENCES; i++) {
        if (boneIds[i] == -1)
            break; //end of bone list
        if (boneIds[i] >= MAX_BONES) //bones contain invalid data -> vertex is not animated
            break;
        if (weights[i] <= 0.0)
            break; //either bone has an unset weight if negative, or no influence at all

        bones++;

        vec4 localPosition = boneMatrices[boneIds[i]] * vec4(iPosition, 1.0);
        bonedPosition += localPosition * weights[i];

        vec3 localNormal = mat3(boneMatrices[boneIds[i]]) * normal * iNormal;
        bonedNormal += localNormal * weights[i];
    }

    if (bones == 0) { //no bones are set, thus vertex is not involved in animation
        bonedPosition = vec4(iPosition, 1.0);
        bonedNormal = iNormal;
    }

    tNormal = normalize(bonedNormal);
    tTextureCoords = iTextureCoords;
    tFragPos = vec3(model * bonedPosition);
    gl_Position = combined * model * bonedPosition;
    tBoneIds = boneIds;
    tWeights = weights;
}