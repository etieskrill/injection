#version 330 core

#pragma stage vertex

const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

varying vec2 texCoords;

void main()
{
    gl_Position = vec4(vertices[gl_VertexID], 0, 1);
    texCoords = max(vertices[gl_VertexID], vec2(0, 0));
}

#pragma stage fragment

varying vec2 texCoords;

out vec4 fragColour;

uniform sampler2D source;
uniform bool horizontal;
uniform vec2 sampleDistance;

const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void sampleWithOffsetPure(inout vec3 offsetSample, vec2 offset) {
    for (int i = 1; i < weights.length(); i++) {
        offsetSample += texture(source, texCoords + offset * sampleDistance * i).rgb * weights[i];
        offsetSample += texture(source, texCoords - offset * sampleDistance * i).rgb * weights[i];
    }
}

void main()
{
    vec2 texOffset = 1.0 / textureSize(source, 0);
    vec3 result = texture(source, texCoords).rgb * weights[0];

    vec2 offset;
    if (horizontal) {
        offset = vec2(texOffset.x, 0);
    } else {
        offset = vec2(0, texOffset.y);
    }

    sampleWithOffsetPure(result, offset);

    fragColour = vec4(result, 1.0);
}
