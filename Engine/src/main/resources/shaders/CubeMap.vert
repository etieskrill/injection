#version 330 core

layout (location = 0) in vec3 iPos;

out vec3 tTextureDir;

uniform mat4 combined;

void main()
{
    tTextureDir = normalize(-iPos);
    gl_Position = combined * vec4(iPos, 1.0);
}
