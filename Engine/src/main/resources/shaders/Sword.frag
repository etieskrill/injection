#version 330 core

#define LIMIT_ATTENUATION true

#define NR_DIRECTIONAL_LIGHTS 1
#define NR_POINT_LIGHTS 2
//#define NR_SPOT_LIGHTS 1

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
    sampler2D diffuse0;
    sampler2D specular0;
    sampler2D shininess0;
    float shininess;
    float specularity;
    sampler2D emissive0;
};

out vec4 oColour;

in vec3 tNormal;
in vec2 tTextureCoords;
in vec3 tFragPos;

uniform vec3 uViewPosition;
uniform float uTime;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];
uniform SpotLight flashlight;
uniform Material material;

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);

void main()
{
    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++)
        combinedLight += calculateDirectionalLight(globalLights[i], tNormal, tFragPos, uViewPosition);
    for (int i = 0; i < NR_POINT_LIGHTS; i++)
        combinedLight += calculatePointLight(lights[i], tNormal, tFragPos, uViewPosition);

    vec4 emission = texture(material.emissive0, tTextureCoords) * material.shininess * (1 / material.shininess);
    combinedLight += emission * (sin(2 * uTime) * 0.1 + 0.5);

    oColour = combinedLight;
}

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec4 ambient = vec4(light.ambient, 1.0) * texture(material.diffuse0, tTextureCoords);

    vec3 lightDirection = normalize(-light.direction);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(light.diffuse, 1.0) * diff * texture(material.diffuse0, tTextureCoords);

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularExponent = texture(material.shininess0, tTextureCoords).r * 256; //a somewhat arbitray factor, wavefront specifies an exponent range from 0 to 1000, but this changes from format to format, and what feels like from model to model
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), specularExponent);
    vec4 specTex = texture(material.specular0, tTextureCoords);
    vec4 specular = vec4(light.specular, 1.0) * spec * specTex;

    return ambient + diffuse + specular;
}

vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec4 ambient = vec4(light.ambient, 1.0) * texture(material.diffuse0, tTextureCoords);

    vec3 lightDirection = normalize(light.position - fragPosition);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(light.diffuse, 1.0) * diff * texture(material.diffuse0, tTextureCoords);

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularExponent = texture(material.shininess0, tTextureCoords).r * 256;
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), specularExponent);
    vec4 specTex = texture(material.specular0, tTextureCoords);
    vec4 specular = vec4(light.specular, 1.0) * spec * specTex;

    float distance = length(lightDirection);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return (ambient + diffuse + specular) * attenuation;
}
