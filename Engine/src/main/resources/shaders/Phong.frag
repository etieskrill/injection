#version 420 core

struct DirectionalLight {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};

struct SpotLight {
    vec3 position;
    vec3 direction;
    float cutoff;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};

struct Material {
    sampler2D diffuse;
    sampler2D specular;
    sampler2D emission;
    float shininess;
};

out vec4 oColour;

in vec3 tNormal;
in vec3 tColour;
in vec2 tTextureCoords;
in vec3 tFragPos;

layout (binding = 0) uniform sampler2D diffuseMap;
layout (binding = 1) uniform sampler2D specularMap;
layout (binding = 2) uniform sampler2D emissionMap;

uniform vec3 uViewPosition;
uniform float uTime;

uniform PointLight light;
uniform SpotLight flashlight;
uniform Material material;

void main() {

    vec3 ambient = light.ambient * texture(material.diffuse, tTextureCoords).rgb;

    vec3 lightDirection = normalize(light.position - tFragPos);
    float diff = max(dot(tNormal, lightDirection), 0.0); //TODO put in vertex
    vec3 diffuse = light.diffuse * diff * texture(material.diffuse, tTextureCoords).rgb;

    vec3 viewDirection = normalize(-uViewPosition - tFragPos);
    vec3 reflectionDirection = reflect(-lightDirection, tNormal);
    float spec = pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    vec3 specular = light.specular * spec * texture(material.specular, tTextureCoords).rgb;

    vec3 emission;
    if (length(texture(material.specular, tTextureCoords).rgb) == 0.0) {
        emission = texture(material.emission, tTextureCoords + vec2(0.0, uTime * 0.25)).rgb;
        emission = emission.grb * 0.7;
    } else {
        emission = vec3(0.0);
    }

    float distance = length(light.position - tFragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    attenuation = min(attenuation, 1.0);

    //Flashlight
    vec3 flashlightPart = vec3(0);

    vec3 flashlightDirection = normalize(vec3(-flashlight.position.xy, flashlight.position.z) - tFragPos);
    float theta = dot(flashlightDirection, normalize(vec3(flashlight.direction.xy, -flashlight.direction.z)));
    if(theta > flashlight.cutoff) {
        /*vec3 flashAmbient = flashlight.ambient * texture(material.diffuse, tTextureCoords).rgb;

        vec3 lightDirection = normalize(flashlight.position - tFragPos);
        float diff = max(dot(tNormal, lightDirection), 0.0); //TODO put in vertex
        vec3 flashDiffuse = flashlight.diffuse * diff * texture(material.diffuse, tTextureCoords).rgb;

        vec3 viewDirection = normalize(-uViewPosition - tFragPos);
        vec3 reflectionDirection = reflect(-lightDirection, tNormal);
        float spec = pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
        vec3 flashSpecular = flashlight.specular * spec * texture(material.specular, tTextureCoords).rgb;

        float distance = 1.0 / length(normalize(flashlight.position - tFragPos));
        //float flashAttenuation = 1.0 / flashlight.constant + flashlight.linear * distance + flashlight.quadratic * distance * distance;
        //flashAttenuation = min(attenuation, 1.0);
        float flashAttenuation = 1.0;

        flashlightPart = (flashAmbient + flashDiffuse + flashSpecular) * flashAttenuation;*/

        //flashlightPart = vec3(1f);
    }

    oColour = vec4((ambient + diffuse + specular) * attenuation + emission + flashlightPart, 1.0);

}
