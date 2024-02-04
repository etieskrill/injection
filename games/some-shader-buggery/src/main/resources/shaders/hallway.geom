#version 330 core

#define SUBDIVISIONS 2

layout (triangles) in;
layout (triangle_strip, max_vertices = 512) out;

out vec2 tTextureCoords;
out vec3 tFragPos;

uniform mat4 uModel;
uniform mat3 uNormal;
uniform mat4 uCombined;

uniform float uTime;

void main()
{

    vec3 v0 = vec3(gl_in[0].gl_Position);
    vec3 v1 = vec3(gl_in[1].gl_Position);
    vec3 v2 = vec3(gl_in[2].gl_Position);

    float lenV0V1 = length(v1 - v0);
    float lenV0V2 = length(v2 - v0);
    float lenV1V2 = length(v2 - v1);

    vec3 chosenV0 = v0, chosenV1 = v1, chosenV2 = v2;
    if (lenV0V1 <= lenV0V2 && lenV0V1 <= lenV1V2) {
        return;
        chosenV0 = v2;
        chosenV1 = v1;
        chosenV2 = v0;
    } else if (lenV0V2 <= lenV0V1 && lenV0V2 <= lenV1V2) {
        //        return;
        chosenV0 = v1;
        chosenV1 = v0;
        chosenV2 = v2;
    } else {
        //        return;
    }

    vec3 d0 = (chosenV1 - chosenV0) / SUBDIVISIONS;
    vec3 d1 = (chosenV2 - chosenV0) / SUBDIVISIONS;

    mat4 mvp = uCombined * uModel;

    gl_Position = mvp * vec4(chosenV0, 1.0);
    tTextureCoords = vec2(0.0);
    tFragPos = vec3(uModel * gl_Position);
    EmitVertex();
    for (int i = 1; i <= SUBDIVISIONS; i++) {
        gl_Position = mvp * vec4(chosenV0 + d0 * i, 1.0);
        tTextureCoords = vec2(i / SUBDIVISIONS, i / SUBDIVISIONS);
        tFragPos = vec3(uModel * gl_Position);
        EmitVertex();
        gl_Position = mvp * vec4(chosenV0 + d1 * i, 1.0);
        tTextureCoords = vec2(0.0, i / SUBDIVISIONS);
        tFragPos = vec3(uModel * gl_Position);
        EmitVertex();
    }

    EndPrimitive();

}
