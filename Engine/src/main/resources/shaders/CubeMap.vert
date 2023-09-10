#version 330 core

layout (location = 0) in vec3 iPos;

out vec3 tTextureDir;

uniform mat4 uCombined;

void main()
{
    tTextureDir = -iPos;
    gl_Position = uCombined * vec4(iPos, 1.0);
}
