#version 410 core

layout (location = 0) in vec3 a_Position;
layout (location = 1) in vec3 a_Normal;
layout (location = 2) in vec2 a_TexCoord;
layout (location = 3) in vec3 a_Tangent;
layout (location = 4) in vec3 a_BiTangent;

out Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoord;
    vec3 fragPos;
    vec4 lightSpaceFragPos;
} vert_out;

uniform mat4 mesh;
uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

uniform vec2 textureScale;

uniform mat4 u_LightCombined;

void main()
{
    vec3 normal = normalize(normal * a_Normal);
    vec3 tangent = normalize(normal * a_Tangent);
    vec3 biTangent = normalize(normal * a_BiTangent);
    vert_out.normal = normal;
    vert_out.tbn = mat3(tangent, biTangent, normal);

    vert_out.texCoord = a_TexCoord * textureScale;
    vert_out.fragPos = vec3(model * vec4(a_Position, 1.0));
    vert_out.lightSpaceFragPos = u_LightCombined * vec4(vert_out.fragPos, 1.0);
    gl_Position = combined * model * vec4(a_Position, 1.0);
}