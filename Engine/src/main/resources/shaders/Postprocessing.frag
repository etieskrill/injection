#version 330 core

out vec4 oColour;

in vec2 tTextureCoords;

struct Material {
    sampler2D diffuse0;
};

vec4 applyKernel(float[9] kernel, float offset);

uniform Material material;

uniform bool invert;
uniform vec3 colour;
uniform bool grayscale;
uniform bool sharpen;
uniform float sharpenOffset;
uniform bool blur;
uniform float blurOffset;
uniform bool edgeDetection;
uniform bool emboss;
uniform float embossOffset;
uniform bool gammaCorrection;
uniform float gammaFactor;

void main()
{
    vec4 texel = texture(material.diffuse0, tTextureCoords);

    texel = invert ? 1 - texel : texel;

    if (grayscale) {
        float avg = 0.2126 * texel.r + 0.7152 * texel.g + 0.0722 * texel.b;
        texel.rgb = vec3(avg);
    }

    float sharpenKernel[9] = float[](
        -1, -1, -1,
        -1,  9, -1,
        -1, -1, -1
    );
    if (sharpen) texel = applyKernel(sharpenKernel, sharpenOffset);

    const float div = 16;
    float blurKernel[9] = float[](
    1 / div, 2 / div, 1 / div,
    2 / div, 4 / div, 2 / div,
    1 / div, 2 / div, 1 / div
    );
    if (blur) texel = applyKernel(blurKernel, blurOffset);

    float edgeDetectionKernel[9] = float[](
    1,  1,  1,
    1, -8,  1,
    1,  1,  1
    );
    if (edgeDetection) texel = applyKernel(edgeDetectionKernel, 1.0 / 2500.0);

    float embossKernel[9] = float[](
    -2, -1,  0,
    -1,  1,  1,
     0,  1,  2
    );
    if (emboss) texel = applyKernel(embossKernel, embossOffset);

    oColour = texel * vec4(colour, 1.0);

    if (gammaCorrection)
    oColour.rgb = pow(oColour.rgb, vec3(1 / (gammaFactor == 0 ? 2.2 : gammaFactor)));
}

vec4 applyKernel(float[9] kernel, float offset) {
    if (offset == 0.0) offset = 1.0 / 1000.0;

    vec2 offsets[9] = vec2[](
        vec2(-offset, offset), // top-left
        vec2(0.0f, offset), // top-center
        vec2(offset, offset), // top-right
        vec2(-offset, 0.0f), // center-left
        vec2(0.0f, 0.0f), // center-center
        vec2(offset, 0.0f), // center-right
        vec2(-offset, -offset), // bottom-left
        vec2(0.0f, -offset), // bottom-center
        vec2(offset, -offset)  // bottom-right
    );

    vec3 sampleTex[9];
    for (int i = 0; i < 9; i++)
        sampleTex[i] = texture(material.diffuse0, tTextureCoords + offsets[i]).rgb;

    vec3 colour = vec3(0.0);
    for (int i = 0; i < 9; i++)
        colour += sampleTex[i] * kernel[i];

    return vec4(colour, 1.0);
}
