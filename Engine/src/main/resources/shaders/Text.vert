#version 330 core

layout (location = 0) in vec2 aGlyphSize;
layout (location = 1) in vec2 aGlyphPosition;
layout (location = 2) in int aGlyphIndex;

out vec2 tGlyphSize;
out vec2 tGlyphTextureSize;
out vec2 tTexRatio;
flat out int tTexIndex;

uniform mat4 uModel;
uniform mat4 uCombined;

uniform vec2 uGlyphTextureSize;

void main()
{
//    gl_Position = vec4((uCombined * uModel * vec4(aGlyphPosition, 0.0, 1.0)).xy, 0.0, 1.0); //Flatten depth
    gl_Position = uCombined * uModel * vec4(aGlyphPosition, 0.0, 1.0);
    tGlyphSize = (uCombined * uModel * vec4(aGlyphSize, 0.0, 1.0)).xy / uGlyphTextureSize;
//    tGlyphSize = (uCombined * uModel * vec4(uGlyphTextureSize, 0.0, 1.0)).xy / uGlyphTextureSize;
    tTexRatio = uGlyphTextureSize / aGlyphSize;
    //    tGlyphSize = aGlyphSize;// * (uCombined * uModel * vec4(vec2(24.0 * (1.0 / 96.0)), 0.0, 1.0)).xy;
    //    tGlyphTextureSize = (uCombined * uModel * vec4(uGlyphTextureSize, 0, 1)).xy;
    tTexIndex = aGlyphIndex;
}
