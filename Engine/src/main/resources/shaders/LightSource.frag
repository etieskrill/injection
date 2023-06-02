#version 330 core

out vec4 oColour;

in vec4 tColour;

uniform vec4 uLightColour;

void main() {

    oColour = uLightColour;

}
