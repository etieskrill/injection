#version 330 core

uniform struct Camera {
    mat4 combined;
} camera;

uniform struct BillBoard {
    sampler2D sprite;
    vec2 size;
    vec3 offset;
} billBoard;

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
    for (int i = 0; i < 4; i++) {
        vec3 cornerOffset =
        //          cameraRotation * //FIXME (if sprites are not just going to be xy-aligned)
        vec3(vec2(-billBoard.size.x, billBoard.size.y) * corners[i], 0);
        cornerOffset.y -= billBoard.size.y;
        vec3 offset = billBoard.offset;
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
    if (texture(billBoard.sprite, texCoords).a == 0) discard;
}
#endif
