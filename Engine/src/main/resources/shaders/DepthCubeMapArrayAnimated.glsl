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

#ifdef GEOMETRY_SHADER

#define NUM_SIDES 6

layout (triangles) in;
layout (triangle_strip, max_vertices = 18) out;

out vec4 fragPos;

uniform mat4 shadowCombined[NUM_SIDES];
uniform int index;

void main()
{
    for (int face = 0; face < NUM_SIDES; face++)
    {
        gl_Layer = 6 * index + face;
        for (int i = 0; i < 3; i++)
        {
            fragPos = gl_in[i].gl_Position;
            gl_Position = shadowCombined[face] * fragPos;
            EmitVertex();
        }
        EndPrimitive();
    }
}

#endif

#ifdef FRAGMENT_SHADER

in vec4 fragPos;

uniform struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
} light;

uniform float farPlane;

void main()
{
    float distance = length(fragPos.xyz - light.position);
    distance = distance / farPlane;
    gl_FragDepth = distance;
}

#endif
