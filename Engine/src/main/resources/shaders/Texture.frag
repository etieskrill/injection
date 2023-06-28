#version 420 core

out vec4 oColour;

in vec3 tColour;
in vec2 tTextureCoords;

layout (binding = 0) uniform sampler2D diffuseMap;

uniform vec3 uColour;
uniform float uTime;

void main() {

    //oColour = vec4(uColour, 1.0);
    oColour = vec4(mix(texture(diffuseMap, tTextureCoords).rgb, uColour, 0.5), 1.0);

}
