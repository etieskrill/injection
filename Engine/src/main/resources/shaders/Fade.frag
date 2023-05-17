#version 420 core

out vec4 oColour; //TODO why in the goddamn giggity fuck does this have to be before the ins

in vec4 tColour;
in vec2 tTextureCoords;

layout (binding = 0) uniform sampler2D texture1;
layout (binding = 1) uniform sampler2D texture2;

void main() {

    oColour = mix(texture(texture1, tTextureCoords), texture(texture2, tTextureCoords), 0.4);
    //oColour = texture(texture1, tTextureCoords) * tColour;
    //oColour = texture(texture1, tTextureCoords) * tColour;

}
