#version 330 core

#define LIMIT_ATTENUATION true
#define SINGLE_CHANNEL_SPECULAR true

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
    float shininess;
    float specularity;
    sampler2D emissive0;
};

out vec4 oColour;

in vec3 tNormal;
in vec2 tTextureCoords;
in vec3 tFragPos;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 uViewPosition;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];
uniform SpotLight flashlight;
uniform Material material;

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);

void main()
{
    vec3 combinedLight = vec3(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++)
        combinedLight += calculateDirectionalLight(globalLights[i], tNormal, tFragPos, uViewPosition);
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        combinedLight += calculatePointLight(lights[i], tNormal, tFragPos, uViewPosition);
    }

    vec3 emission = texture(material.emissive0, tTextureCoords).rgb;
    combinedLight += emission;

    oColour = vec4(combinedLight, 1.0);
}

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 ambient = light.ambient * texture(material.diffuse0, tTextureCoords).rgb;

    vec3 lightDirection = normalize(-light.direction);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = light.diffuse * diff * texture(material.diffuse0, tTextureCoords).rgb;

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    vec4 specTex = texture(material.specular0, tTextureCoords);
    vec3 specular = light.specular * spec * (SINGLE_CHANNEL_SPECULAR ? specTex.rrr : specTex.rgb);

    return ambient + diffuse + specular;
}

vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 ambient = light.ambient * texture(material.diffuse0, tTextureCoords).rgb;

    vec3 lightDirection = normalize(light.position - fragPosition);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = light.diffuse * diff * texture(material.diffuse0, tTextureCoords).rgb;

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    vec4 specTex = texture(material.specular0, tTextureCoords);
    vec3 specular = light.specular * spec * (SINGLE_CHANNEL_SPECULAR ? specTex.rrr : specTex.rgb);

    float distance = length(lightDirection);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return (ambient + diffuse + specular) * attenuation;
}
