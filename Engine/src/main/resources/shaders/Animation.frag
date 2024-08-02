#version 330 core

#define LIMIT_ATTENUATION true

#define NR_DIRECTIONAL_LIGHTS 1
#define NR_POINT_LIGHTS 2

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

struct Material {
    sampler2D diffuse0;
    bool specularTexture;
    sampler2D specular0;
    float shininess;
    float specularity;
    sampler2D normal0;
    bool emissiveTexture;
    sampler2D emissive0;
    samplerCube cubemap0;
};

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

in vec3 tNormal;
uniform bool normalMapped;
in mat3 tbn;
in vec2 tTextureCoords;
in vec3 tFragPos;

flat in ivec4 tBoneIds;
flat in vec4 tWeights;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 viewPosition;

uniform Material material;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform int uShowBoneSelector;
uniform bool uShowBoneWeights;

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);
vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);

vec4 getAmbient(vec3 lightAmbient);
vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular);

//TODO shadow mapping
void main()
{
    if (texture(material.diffuse0, tTextureCoords).a == 0) discard;

    vec3 normal;
    if (normalMapped) {
        normal = texture(material.normal0, tTextureCoords).xyz * 2.0 - 1.0;
        normal = normalize(tbn * normalize(normal));
    } else {
        normal = tNormal;
    }

    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        vec4 dirLight = getDirLight(globalLights[i], normal, tFragPos, viewPosition, 1);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec4 pointLight = getPointLight(lights[i], normal, tFragPos, viewPosition, 1);
        combinedLight += pointLight;
    }

    if (material.emissiveTexture) {
        vec4 emissive = texture(material.emissive0, tTextureCoords);
        combinedLight += vec4(emissive.rgb, 0);
    }

    fragColour = combinedLight;

    float brightness = dot(combinedLight.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = combinedLight;
    else bloomColour = vec4(0.0, 0.0, 0.0, 1.0);

    if (uShowBoneSelector != 4) {
        if (tBoneIds[uShowBoneSelector] % 3 == 0) fragColour.x = (float(tBoneIds[uShowBoneSelector]) / 100.0);
        if (tBoneIds[uShowBoneSelector] % 3 == 1) fragColour.y = (float(tBoneIds[uShowBoneSelector]) / 100.0);
        if (tBoneIds[uShowBoneSelector] % 3 == 2) fragColour.z = (float(tBoneIds[uShowBoneSelector]) / 100.0);
    }
    if (uShowBoneWeights) fragColour = tWeights;
}

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    float distance = length(light.position - fragPosition);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return vec4((ambient + inShadow * (diffuse + specular)).rgb * attenuation, 1.0);
}

vec4 getAmbient(vec3 lightAmbient)
{
    vec4 ambient = vec4(lightAmbient, 1.0);
    return ambient * texture(material.diffuse0, tTextureCoords);
}

vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff;
    return diffuse * texture(material.diffuse0, tTextureCoords);
}

vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularFactor;

    vec3 halfway = normalize(lightDirection + viewDirection);
    specularFactor = max(dot(normal, halfway), 0.0);

    float specularIntensity = material.specularity * pow(specularFactor, material.shininess);
    vec3 specular = lightSpecular;
    if (material.specularTexture) specular *= texture(material.specular0, tTextureCoords).rgb;
    return vec4(specular * specularIntensity, 1.0);
}
