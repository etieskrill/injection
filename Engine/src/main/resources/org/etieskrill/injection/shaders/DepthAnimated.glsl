#version 330 core

#ifdef VERTEX_SHADER

#define MAX_BONES 100
#define MAX_BONE_INFLUENCES 4

layout (location = 0) in vec3 a_Position;
layout (location = 5) in ivec4 a_BoneIds;
layout (location = 6) in vec4 a_BoneWeights;

uniform mat4 model;
uniform mat4 combined;

uniform mat4 boneMatrices[MAX_BONES];

void main()
{
    vec4 bonedPosition = vec4(0.0);

    int bones = 0;
    for (int i = 0; i < MAX_BONE_INFLUENCES; i++) {
        if (a_BoneIds[i] == -1) break; //end of bone list
        if (a_BoneIds[i] >= MAX_BONES) break; //bones contain invalid data -> vertex is not animated
        if (a_BoneWeights[i] <= 0.0) break; //either bone has an unset weight if negative, or no influence at all

        bones++;

        vec4 localPosition = boneMatrices[a_BoneIds[i]] * vec4(a_Position, 1.0);
        bonedPosition += localPosition * a_BoneWeights[i];
    }

    if (bones == 0) { //no bones are set, thus vertex is not involved in animation
        bonedPosition = vec4(a_Position, 1.0);
    }

    gl_Position = combined * model * bonedPosition;
}

#endif

#ifdef FRAGMENT_SHADER

void main()
{
}

#endif
