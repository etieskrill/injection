#version 420 core

out vec4 oColour; //TODO why in the goddamn giggity fuck does this have to be before the ins

in vec3 tNormal;
in vec4 tColour;
in vec2 tTextureCoords;
in vec3 tFragPos;

layout (binding = 0) uniform sampler2D texture1;
layout (binding = 1) uniform sampler2D texture2;

uniform vec3 uObjectColour;
uniform vec4 uLightColour;
uniform float uAmbientStrength;
uniform vec3 uLightPosition;

void main() {

    vec4 ambient = uAmbientStrength * uLightColour;
    ambient.w = 1.0;

    vec3 normal = normalize(tNormal);
    vec3 lightDirection = normalize(uLightPosition - tFragPos);
    float diffuseStrength = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = diffuseStrength * uLightColour.xyz;

    vec4 texturedColour = uLightColour * tColour + mix(texture(texture1, tTextureCoords), texture(texture2, tTextureCoords), 0.4);

    oColour = vec4((vec3(ambient) + diffuse) * vec3(texturedColour), 1.0);
    //oColour = mix(texture(texture1, tTextureCoords), texture(texture2, tTextureCoords), 0.4);
    //oColour = texture(texture1, tTextureCoords) * tColour;
    //oColour = texture(texture1, tTextureCoords) * tColour;

}
