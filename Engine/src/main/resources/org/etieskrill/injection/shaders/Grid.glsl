#version 330 core

uniform struct Camera {
    mat4 combined;
    vec3 position;
    float far;
} camera;

#pragma stage vert
void main()
{}

#pragma stage geom
layout (points) in;
layout (triangle_strip, max_vertices = 12) out;

out vec4 fragPos;

uniform vec3 position;

const vec2 corners[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

void main()
{
    for (int i = 0; i < 4; i++) {
        fragPos = vec4(position, 1.0);
        fragPos.yz += camera.far * corners[i] + camera.position.yz;
        gl_Position = camera.combined * fragPos;
        EmitVertex();
    }
    EndPrimitive();
    for (int i = 0; i < 4; i++) {
        fragPos = vec4(position, 1.0);
        fragPos.xz += camera.far * corners[i] + camera.position.xz;
        gl_Position = camera.combined * fragPos;
        EmitVertex();
    }
    EndPrimitive();
    for (int i = 0; i < 4; i++) {
        fragPos = vec4(position, 1.0);
        fragPos.xy += camera.far * corners[i] + camera.position.xy;
        gl_Position = camera.combined * fragPos;
        EmitVertex();
    }
    EndPrimitive();
}

#pragma stage frag
in vec4 fragPos;

out vec4 fragColour;

bool isCardinalGrid(float number, float width) {
    return abs(number) < width / 2;
}

bool isCardinalGrid(vec2 number, float width) {
    return isCardinalGrid(number.x, width) && isCardinalGrid(number.y, width);
}

bool isGrid(float number, float width) {
    return abs(fract(number)) < width / 2;
}

bool isGrid(vec2 number, float width) {
    return isGrid(number.x, width) && isGrid(number.y, width);
}

void main() {
    if (fragPos.y != 0) {
        discard;
    }

    float dist = length(fragPos.xyz - camera.position);
    float cardinalWidth = 0.003 * dist;
    float width = 0.003 * dist;
    float alpha = min(1, 10 / dist);

    if (isCardinalGrid(fragPos.yz, cardinalWidth)) {
        fragColour = vec4(0.3, 0.0, 0.0, alpha);
    } else if (isCardinalGrid(fragPos.xz, cardinalWidth)) {
        fragColour = vec4(0.0, 0.3, 0.0, alpha);
    } else if (isCardinalGrid(fragPos.xy, cardinalWidth)) {
        fragColour = vec4(0.0, 0.0, 0.3, alpha);
    } else if (isGrid(fragPos.xy, width) || isGrid(fragPos.yz, width) || isGrid(fragPos.xz, width)) {
        fragColour = vec4(0.2, 0.2, 0.2, alpha);
    } else {
        discard;
    }

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}
