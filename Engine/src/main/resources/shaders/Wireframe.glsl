#version 330 core

#ifdef VERTEX_SHADER
layout (location = 0) in vec3 a_Pos;

uniform mat4 model;
uniform mat4 combined;

void main()
{
    gl_Position = combined * model * vec4(a_Pos, 1.0);
}
#endif

#ifdef FRAGMENT_SHADER
layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

uniform vec4 colour;

void main()
{
    fragColour = colour;
    bloomColour = vec4(0.0, 0.0, 0.0, 1.0);
}
#endif
