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

varying Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoords;
    vec3 fragPos;

    flat ivec4 boneIds;
    flat vec4 boneWeights;
} vertex;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 viewPosition;

uniform Material material;
uniform bool normalMapped;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform int uShowBoneSelector;
uniform bool uShowBoneWeights;

vec4 getDirLight(DirectionalLight light, vec3 normal, float inShadow);
vec4 getPointLight(PointLight light, vec3 normal, float inShadow);

vec4 getAmbient(vec3 lightAmbient);
vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 lightSpecular);

//TODO shadow mapping
void main()
{
    if (texture(material.diffuse0, vertex.texCoords).a == 0) discard;

    vec3 normal;
    if (normalMapped) {
        normal = texture(material.normal0, vertex.texCoords).xyz * 2.0 - 1.0;
        normal = normalize(vertex.tbn * normalize(normal));
    } else {
        normal = vertex.normal;
    }

    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        vec4 dirLight = getDirLight(globalLights[i], normal, 1);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec4 pointLight = getPointLight(lights[i], normal, 1);
        combinedLight += pointLight;
    }

    if (material.emissiveTexture) {
        vec4 emissive = texture(material.emissive0, vertex.texCoords);
        combinedLight += vec4(emissive.rgb * emissive.rgb, 0);
    }

    fragColour = combinedLight;

    float brightness = dot(combinedLight.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = combinedLight;
    else bloomColour = vec4(0.0, 0.0, 0.0, 1.0);

    if (uShowBoneSelector != 4) {
        if (vertex.boneIds[uShowBoneSelector] % 3 == 0) fragColour.x = (float(vertex.boneIds[uShowBoneSelector]) / 100.0);
        if (vertex.boneIds[uShowBoneSelector] % 3 == 1) fragColour.y = (float(vertex.boneIds[uShowBoneSelector]) / 100.0);
        if (vertex.boneIds[uShowBoneSelector] % 3 == 2) fragColour.z = (float(vertex.boneIds[uShowBoneSelector]) / 100.0);
    }
    if (uShowBoneWeights) fragColour = vertex.boneWeights;
}

vec4 getDirLight(DirectionalLight light, vec3 normal, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, normal, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec4 getPointLight(PointLight light, vec3 normal, float inShadow)
{
    vec3 lightDirection = normalize(light.position - vertex.fragPos);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, normal, light.specular);

    float distance = length(light.position - vertex.fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return vec4((ambient + inShadow * (diffuse + specular)).rgb * attenuation, 1.0);
}

vec4 getAmbient(vec3 lightAmbient)
{
    vec4 ambient = vec4(lightAmbient, 1.0);
    return ambient * texture(material.diffuse0, vertex.texCoords);
}

vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff;
    return diffuse * texture(material.diffuse0, vertex.texCoords);
}

vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - vertex.fragPos);
    float specularFactor;

    vec3 halfway = normalize(lightDirection + viewDirection);
    specularFactor = max(dot(normal, halfway), 0.0);

    float shininess = material.shininess > 0 ? material.shininess : 64;
    float specularIntensity = material.specularity * pow(specularFactor, shininess);
    vec3 specular = specularIntensity * lightSpecular;
    if (material.specularTexture) specular *= texture(material.specular0, vertex.texCoords).rgb;
    return vec4(specular, 1.0);
}
