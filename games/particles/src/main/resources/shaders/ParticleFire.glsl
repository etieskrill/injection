#version 410 core

#ifdef VERTEX_SHADER
layout (location = 0) attribute vec3 a_Position;
layout (location = 1) attribute vec4 a_Colour;

flat out vec4 colour;

uniform float size;

uniform mat4 view;
uniform mat4 projection;

uniform ivec2 screenSize;

void main()
{
    vec4 eyePosition = view * vec4(a_Position, 1.0);
    float dist = distance(a_Position, eyePosition.xyz);
    //    vec4 projectionVoxel = projection * vec4(1, 1, eyePosition.z, eyePosition.w);
    //    vec2 projectionSize = screenSize * (projectionVoxel.xy / projectionVoxel.w);
    //    gl_PointSize = 0.25 * (projectionSize.x + projectionSize.y);
    //    gl_Position = projection * eyePosition;
    gl_PointSize = 1000.0 * (size / dist);
    gl_Position = projection * view * vec4(a_Position, 1.0);
    colour = a_Colour;
}
#endif

#ifdef FRAGMENT_SHADER
flat in vec4 colour;

out vec4 fragColour;

uniform sampler2D sprite;

void main()
{
    fragColour = texture(sprite, gl_PointCoord) * colour * 2;
    if (fragColour.a < 0.1) discard;
}
#endif
