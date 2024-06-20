#version 330 core

layout (location = 0) in vec2 aGlyphSize;
layout (location = 1) in vec2 aGlyphPosition;
layout (location = 2) in int aGlyphIndex;

out vec2 tGlyphSize;
flat out int tTexIndex;

uniform mat4 combined;

uniform vec2 glyphTextureSize;

void main()
{
    gl_Position = vec4(aGlyphPosition, 0.0, 1.0);
    tGlyphSize = aGlyphSize;
    tTexIndex = aGlyphIndex;
}
