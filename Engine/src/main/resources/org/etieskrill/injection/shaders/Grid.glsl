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

void main() {
    if (fragPos.y != 0) {
        discard;
    }

    float dist = length(fragPos.xyz - camera.position);
    float cardinalWidth = 0.005;
    float width = 0.001;
    float alpha = min(1, 10 / dist);
    vec2 aa = 2 * fwidth(fragPos.xz);

    vec2 grid = smoothstep(width + aa, width - aa, abs(vec2(fragPos.xz) % 1.0));
    grid += smoothstep(width - aa, width + aa, abs(vec2(fragPos.xz) % 1.0) - 1.0 + 2 * width);
    float lines = (grid.x + grid.y) * (1 - smoothstep(camera.far * 1/2, camera.far, dist));
    vec3 gridColour;
    if (abs(fragPos.x) < aa.x) {
        gridColour = vec3(1, 0, 0);
    } else if (abs(fragPos.z) < aa.y) {
        gridColour = vec3(0, 1, 0);
    }
//    else if (abs(fragPos.z) < cardinalWidth) {
//        gridColour = vec3(0, 0, 1);
//    }
    else {
        gridColour = vec3(0.25);
    }
    fragColour = vec4(gridColour, lines);

    fragColour = vec4(pow(fragColour.xyz, vec3(1 / 2.2)), fragColour.a);
}
