#version 420 core

out vec4 oColour; //TODO why in the goddamn giggity fuck does this have to be before the ins

in vec3 tNormal;
in vec4 tColour;
in vec2 tTextureCoords;
in vec3 tFragPos;

layout (binding = 0) uniform sampler2D texture1;
layout (binding = 1) uniform sampler2D texture2;

uniform vec4 uLightColour;
uniform float uAmbientStrength;
uniform vec3 uLightPosition;
uniform vec3 uViewPosition;
uniform float uSpecularStrength;
uniform float uSpecularComponent;

void main() {

    vec3 ambient = uAmbientStrength * vec3(uLightColour);

    vec3 lightDirection = normalize(uLightPosition - tFragPos);
    float diffuseStrength = max(dot(tNormal, lightDirection), 0.0);
    vec3 diffuse = diffuseStrength * uLightColour.xyz;

    vec3 viewDirection = normalize(-uViewPosition - tFragPos);
    vec3 reflectionDirection = reflect(-lightDirection, tNormal);
    float spec = pow(max(dot(viewDirection, reflectionDirection), 0.0), uSpecularComponent);
    vec3 specular = uSpecularStrength * spec * vec3(uLightColour);

    vec4 texturedColour = uLightColour * tColour + mix(texture(texture1, tTextureCoords), texture(texture2, tTextureCoords), 0.4);

    oColour = vec4((ambient + diffuse + specular) * vec3(texturedColour), 1.0);

}
