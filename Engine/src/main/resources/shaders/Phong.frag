#version 400 core

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

in Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoord;
    vec3 fragPos;
    vec4 lightSpaceFragPos;
} vert_out;

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

uniform vec3 viewPosition;
uniform mat3 normal;

uniform bool blinnPhong;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform sampler2DShadow shadowMap;
uniform samplerCubeArrayShadow pointShadowMaps;

uniform float pointShadowFarPlane;

vec3 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);
vec3 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);

vec3 getAmbient(vec3 lightAmbient);
vec3 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec3 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular);

vec4 getCubeReflection(vec3 normal);
vec4 getCubeRefraction(float refractIndex, vec3 normal);

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection);
float getInPointShadow(int index, vec3 fragToLight);

void main()
{
    vec3 normalVec;
    if (material.hasNormalMap) {
        normalVec = texture(material.normal0, vert_out.texCoord).xyz * 2.0 - 1.0;
        normalVec = normalize(normalVec);
        normalVec = normalize(vert_out.tbn * normalVec);
    } else {
        normalVec = vert_out.normal;
    }

    vec4 texel = texture(material.diffuse0, vert_out.texCoord);
    if (texel.a == 0.0) discard;

    vec3 combinedLight = vec3(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        float inShadow = getInShadow(vert_out.lightSpaceFragPos, globalLights[i].direction);
        vec3 dirLight = getDirLight(globalLights[i], normalVec, vert_out.fragPos, viewPosition, inShadow);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        float inShadow = getInPointShadow(i, vert_out.fragPos - lights[i].position);
        vec3 pointLight = getPointLight(lights[i], normalVec, vert_out.fragPos, viewPosition, inShadow);
        combinedLight += pointLight;
    }

    if (material.emissiveTexture) {
        combinedLight += texture(material.emissive0, vert_out.texCoord).rgb;
    }

    //TODO pack reflection mix factor into material property. until reflection maps are a thing, anyway
    //    combinedLight += getCubeReflection(normalVec);

    //    if (combinedLight.a < 0.9) {
    //        combinedLight = mix(combinedLight, getCubeRefraction(1 / 1.52, normalVec), 0.5);
    //    }

    fragColour = vec4(combinedLight, texel.a);

    float brightness = dot(combinedLight.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 2.0) bloomColour = vec4(combinedLight, texel.a);
    else bloomColour = vec4(0.0, 0.0, 0.0, texel.a);
}

vec3 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, lightDirection, normal, fragPosition, vec3(0.0), viewPosition, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec3 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, light.position, normal, fragPosition, fragPosition, viewPosition, light.specular);

    float distance = length(light.position - fragPosition);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return (ambient + inShadow * (diffuse + specular)) * attenuation;
}

vec3 getAmbient(vec3 lightAmbient)
{
    return lightAmbient * texture(material.diffuse0, vert_out.texCoord).rgb;
}

vec3 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = lightDiffuse * diff;
    return diffuse * texture(material.diffuse0, vert_out.texCoord).rgb;
}

vec3 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularFactor;
    if (blinnPhong) {
        vec3 lightDirection = normalize(lightPosition - lightDirFragPosition);
        vec3 halfway = normalize(lightDirection + viewDirection);
        specularFactor = dot(normal, halfway);
    } else {
        vec3 reflectionDirection = reflect(-lightDirection, normal);
        specularFactor = dot(viewDirection, reflectionDirection);
    }

    float shininess = material.shininess > 0 ? material.shininess : 64;
    float specularIntensity = material.specularity * pow(max(specularFactor, 0.0), shininess);
    vec3 specular = lightSpecular * specularIntensity;
    if (material.specularTexture) specular *= texture(material.specular0, vert_out.texCoord).rgb;
    return specular;
}

vec4 getCubeReflection(vec3 normal) {
    vec3 viewDirection = normalize(vert_out.fragPos - viewPosition);
    vec3 viewReflection = reflect(viewDirection, normal);
    return texture(material.cubemap0, -viewReflection);
}

vec4 getCubeRefraction(float refractIndex, vec3 normal) {
    vec3 viewDirection = normalize(vert_out.fragPos - viewPosition);
    vec3 viewRefraction = refract(viewDirection, normal, refractIndex);
    return texture(material.cubemap0, -viewRefraction);
}

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection) {
    vec3 screenSpace = lightSpaceFragPos.xyz / lightSpaceFragPos.w;
    vec3 depthSpace = screenSpace * 0.5 + 0.5;

    float currentDepth = depthSpace.z;
    if (currentDepth > 1.0) return 1.0;

    float bias = min(0.005, 0.05 * (1.0 - dot(vert_out.normal, lightDirection)));
    depthSpace.z -= bias;
    float shadow = 0.0;

    vec3 texelSize = vec3(1.0 / textureSize(shadowMap, 0), 1.0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            shadow += texture(shadowMap, depthSpace + vec3(x, y, 0.0) * texelSize);
        }
    }

    return shadow / 9.0;
}

float getInPointShadow(int index, vec3 fragToLight) {
    float currentDepth = length(fragToLight) / pointShadowFarPlane;

    float bias = min(0.005, 0.05 * (1.0 - dot(vert_out.normal, fragToLight)));
    currentDepth -= bias;

    float shadow = 0.0;
    float offset = 0.01;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                shadow += texture(pointShadowMaps, vec4(fragToLight + vec3(x, y, z) * offset, index), currentDepth);
            }
        }
    }

    return shadow / 27.0;
}
