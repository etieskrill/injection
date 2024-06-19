#version 330 core

layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

in vec2 tGlyphSize[1];
flat in int tTexIndex[1];

out vec2 oTexCoords;
flat out int oTexIndex;

uniform mat4 combined;

uniform vec2 uGlyphTextureSize;

//This is probably less efficient than calculating four vertices in the cpu and doing view transformation in the vertex
//shader, but if this actually becomes a bottleneck, then either something has gone horribly wrong or terribly right
//According to http://www.joshbarczak.com/blog/?p=667, this might actually be faster on certain systems
void main()
{
    vec2 position = gl_in[0].gl_Position.xy;
    vec2 glyphSize = tGlyphSize[0];
    vec2 glyphTexSize = glyphSize / uGlyphTextureSize;
    oTexIndex = tTexIndex[0];

    gl_Position = combined * vec4(position, 0, 1);
    oTexCoords = vec2(0.0);
    EmitVertex();

    gl_Position = combined * vec4(position.x + glyphSize.x, position.y, 0.0, 1.0);
    oTexCoords = vec2(glyphTexSize.x, 0.0);
    EmitVertex();

    gl_Position = combined * vec4(position.x, position.y + glyphSize.y, 0.0, 1.0);
    oTexCoords = vec2(0.0, glyphTexSize.y);
    EmitVertex();

    gl_Position = combined * vec4(position.x + glyphSize.x, position.y + glyphSize.y, 0.0, 1.0);
    oTexCoords = vec2(glyphTexSize);
    EmitVertex();
}
