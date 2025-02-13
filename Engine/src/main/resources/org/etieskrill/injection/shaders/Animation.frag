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

uniform struct Material {
    sampler2D diffuse0;
    bool specularTexture;
    sampler2D specular0;
    float shininess;
    float specularity;
    bool hasNormalMap;
    sampler2D normal0;
    bool emissiveTexture;
    sampler2D emissive0;
    samplerCube cubemap0;
} material;

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

in Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoords;
    vec3 fragPos;

    flat ivec4 boneIds;
    flat vec4 boneWeights;
} vertex;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 viewPosition;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform /*@Range(from = 0, to = 4)*/ int showBoneSelector;
uniform bool showBoneWeights;

vec3 getDirLight(DirectionalLight light, vec3 normal, float inShadow);
vec3 getPointLight(PointLight light, vec3 normal, float inShadow);

vec3 getAmbient(vec3 lightAmbient);
vec3 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec3 getSpecular(vec3 lightDirection, vec3 normal, vec3 lightSpecular);

//TODO shadow mapping
void main()
{
    vec4 texel = texture(material.diffuse0, vertex.texCoords);
    if (texel.a == 0) discard;

    vec3 normal;
    if (material.hasNormalMap) {
        normal = texture(material.normal0, vertex.texCoords).xyz * 2.0 - 1.0;
        normal = normalize(vertex.tbn * normalize(normal));
    } else {
        normal = vertex.normal;
    }

    vec3 combinedLight = vec3(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        vec3 dirLight = getDirLight(globalLights[i], normal, 1);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec3 pointLight = getPointLight(lights[i], normal, 1);
        combinedLight += pointLight;
    }

    if (material.emissiveTexture) {
        combinedLight += texture(material.emissive0, vertex.texCoords).rgb;
    }

    fragColour = vec4(combinedLight, texel.a);

    float brightness = dot(combinedLight.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = vec4(combinedLight, texel.a);
    else bloomColour = vec4(0.0, 0.0, 0.0, texel.a);

    if (showBoneSelector != 4) {
        if (vertex.boneIds[showBoneSelector] % 3 == 0) fragColour.x = (float(vertex.boneIds[showBoneSelector]) / 100.0);
        if (vertex.boneIds[showBoneSelector] % 3 == 1) fragColour.y = (float(vertex.boneIds[showBoneSelector]) / 100.0);
        if (vertex.boneIds[showBoneSelector] % 3 == 2) fragColour.z = (float(vertex.boneIds[showBoneSelector]) / 100.0);
    }
    if (showBoneWeights) fragColour = vertex.boneWeights;
}

vec3 getDirLight(DirectionalLight light, vec3 normal, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, normal, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec3 getPointLight(PointLight light, vec3 normal, float inShadow)
{
    vec3 lightDirection = normalize(light.position - vertex.fragPos);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, normal, light.specular);

    float distance = length(light.position - vertex.fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return (ambient + inShadow * (diffuse + specular)) * attenuation;
}

vec3 getAmbient(vec3 lightAmbient)
{
    return lightAmbient * texture(material.diffuse0, vertex.texCoords).rgb;
}

vec3 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = vec3(lightDiffuse) * diff;
    return diffuse * texture(material.diffuse0, vertex.texCoords).rgb;
}

vec3 getSpecular(vec3 lightDirection, vec3 normal, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - vertex.fragPos);
    float specularFactor;

    vec3 halfway = normalize(lightDirection + viewDirection);
    specularFactor = max(dot(normal, halfway), 0.0);

    float shininess = material.shininess > 0 ? material.shininess : 64;
    float specularIntensity = material.specularity * pow(specularFactor, shininess);
    vec3 specular = specularIntensity * lightSpecular;
    if (material.specularTexture) specular *= texture(material.specular0, vertex.texCoords).rgb;
    return specular;
}
