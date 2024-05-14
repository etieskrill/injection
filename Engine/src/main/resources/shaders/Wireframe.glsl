#version 330 core

#ifdef VERTEX_SHADER
layout (location = 0) in vec3 a_Pos;

uniform mat4 uModel;
uniform mat4 uCombined;

void main()
{
    gl_Position = uCombined * uModel * vec4(a_Pos, 1.0);
}
#endif

#ifdef FRAGMENT_SHADER
out vec4 fragColour;

uniform vec4 colour;

void main()
{
    fragColour = colour;
}
#endif
