#version 410 core

#define MAX_BONES 100
#define MAX_BONE_INFLUENCES 4

layout (location = 0) in vec3 iPosition;
layout (location = 1) in vec3 iNormal;
layout (location = 2) in vec2 iTextureCoords;
layout (location = 3) in ivec4 boneIds;
layout (location = 4) in vec4 weights;

out vec3 tNormal;
out vec2 tTextureCoords;
out vec3 tFragPos;

uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

uniform mat4 boneMatrices[MAX_BONES];

void main()
{
    vec4 bonedPosition = vec4(0.0);
    vec3 bonedNormal = vec3(0.0);

    int bones = 0;
    for (int i = 0; i < MAX_BONE_INFLUENCES; i++) {
        if (boneIds[i] < 0)  //TODO int is probs being read as float and thus much more negative yate yate yate ... replace with == -1 once i have been be fucked to debug this shit
            continue; //no bone set
        if (boneIds[i] >= MAX_BONES) //bones contain invalid data -> vertex is not animated
            break;

        bones++;

        vec4 localPosition = boneMatrices[boneIds[i]] * vec4(iPosition, 1.0);
        bonedPosition += localPosition * weights[i];

        vec3 localNormal = mat3(boneMatrices[boneIds[i]]) * uNormal * iNormal;
        bonedNormal += localNormal * weights[i];
    }

    if (bones == 0) { //no bones are set, thus vertex is not involved in animation
        bonedPosition = vec4(iPosition, 1.0);
        bonedNormal = iNormal;
    }

    tNormal = normalize(bonedNormal);
    tTextureCoords = iTextureCoords;
    tFragPos = vec3(uModel * bonedPosition);
    gl_Position = uCombined * uModel * bonedPosition;
}