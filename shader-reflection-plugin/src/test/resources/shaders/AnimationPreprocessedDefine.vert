#version 410 core




layout
(
location
=
0
)
attribute
vec3
a_Position
;
layout (location = 1) attribute vec3 a_Normal; //asdf
layout (location = 2) attribute vec2 a_TexCoords; // asdf
layout (location = 3) attribute vec3 a_Tangent;//asdf
layout (location = 4) attribute vec3 a_BiTangent;// asdf
layout (location = 5) attribute ivec4 a_BoneIds; /////////////// WWWWWWWWWEEEEEEEEEEEEEEEEEEEE
layout (location = 6) attribute vec4 a_BoneWeights;

out Data {
    vec3 /* asdf*/ normal;
    mat3 tbn;
    vec2 texCoords;
    vec3 fragPos;

    flat ivec4 boneIds;
    flat vec4 boneWeights;
} /*asdf*/               vertex;

uniform

mat4 /* asdf */ model;
uniform mat3 normal;
uniform mat4 combined;

uniform
mat4
boneMatrices
[
100
]
;

uniform bool normalMapped;

void main()
{
    vec4 bonedPosition = vec4(0.0);

    vec3 bonedNormal = vec3(0.0);
    vec3 bonedTangent = vec3(0.0);
    vec3 bonedBiTangent = vec3(0.0);

    int bones = 0;
    for (int i = 0; i < 4; i++) {
        if (a_BoneIds[i] == -1) break; //end of bone list
        if (a_BoneIds[i] >= 100) break; //bones contain invalid data -> vertex is not animated
        if (a_BoneWeights[i] <= 0.0) break; //either bone has an unset weight if negative, or no influence at all

        bones++;

        vec4 localPosition = boneMatrices[a_BoneIds[i]] * vec4(a_Position, 1.0);
        bonedPosition += localPosition * a_BoneWeights[i];

        vec3 localNormal = mat3(boneMatrices[a_BoneIds[i]]) * a_Normal;
        bonedNormal += localNormal * a_BoneWeights[i];
        if (normalMapped) {
            vec3 localTangent = mat3(boneMatrices[a_BoneIds[i]]) * a_Tangent;
            bonedTangent += localTangent * a_BoneWeights[i];
            vec3 localBiTangent = mat3(boneMatrices[a_BoneIds[i]]) * a_BiTangent;
            bonedBiTangent += localBiTangent * a_BoneWeights[i];
        }
    }

    if (bones == 0) { //no bones are set, thus vertex is not involved in animation
                      bonedPosition = vec4(a_Position, 1.0);
                      bonedNormal = a_Normal;
    }

    vertex.normal = normalize(normal * bonedNormal);
    if (normalMapped) {
        vec3 tangent = normalize(normal * bonedTangent);
        vec3 biTangent = normalize(normal * bonedBiTangent);
        vertex.tbn = mat3(tangent, biTangent, vertex.normal);
    }

    vertex.texCoords = a_TexCoords;
    vertex.fragPos = vec3(model * bonedPosition);
    gl_Position = combined * model * bonedPosition;
    vertex.boneIds = a_BoneIds;
    vertex.boneWeights = a_BoneWeights;
}
