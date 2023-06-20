#version 420 core

out vec4 oColour;

in vec3 tColour;
in vec2 tTextureCoords;

layout (binding = 0) uniform sampler2D diffuseMap;

uniform float uTime;

void main() {

    oColour = texture(diffuseMap, tTextureCoords);

}
