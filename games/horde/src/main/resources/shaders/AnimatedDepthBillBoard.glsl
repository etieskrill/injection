#version 330 core

uniform struct Camera {
    mat4 combined;
} camera;

uniform struct AnimatedBillBoard {
    sampler2DArray sprite;
    int layer;
    vec2 size;
    vec3 offset;
    float rotation;
} animatedBillBoard;

#ifdef VERTEX_SHADER
void main()
{}
#endif

#ifdef GEOMETRY_SHADER
layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

out vec2 texCoords;

uniform vec3 position;
uniform mat3 cameraRotation;

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    float rotSin = sin(animatedBillBoard.rotation);
    float rotCos = cos(animatedBillBoard.rotation);
    mat2 rotation = mat2(rotCos, rotSin, -rotSin, rotCos);

    for (int i = 0; i < 4; i++) {
        vec3 cornerOffset =
        //          cameraRotation * //FIXME (if sprites are not just going to be xy-aligned)
        vec3(rotation * (vec2(-animatedBillBoard.size.x, animatedBillBoard.size.y) * corners[i]), 0);
        cornerOffset.y -= animatedBillBoard.size.y;

        vec3 offset = animatedBillBoard.offset;
        offset.y = -offset.y;

        vec4 cornerPos = camera.combined * vec4(position + offset + cornerOffset, 1);

        gl_Position = cornerPos;
        texCoords = (corners[i] / 2.0) + 0.5;
        EmitVertex();
    }
    EndPrimitive();
}
#endif

#ifdef FRAGMENT_SHADER
in vec2 texCoords;

void main() {
    if (texture(animatedBillBoard.sprite, vec3(texCoords, animatedBillBoard.layer)).a == 0) discard;
}
#endif
