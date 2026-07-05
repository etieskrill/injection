#version 410 core

#ifdef VERTEX_SHADER

layout (location = 0) attribute vec3 a_Position;
layout (location = 1) attribute vec3 a_Normal;

out Data {
    vec3 normal;
    vec3 fragPos;
    vec4 lightSpaceFragPos;
} vert_out;

uniform mat4 model;
uniform mat4 mesh;
uniform mat3 normal;
uniform mat4 combined;

uniform mat4 lightCombined;

void main()
{
    vert_out.normal = normalize(normal * a_Normal);

    vert_out.fragPos = vec3(model * mesh * vec4(a_Position, 1.0));
    vert_out.lightSpaceFragPos = lightCombined * vec4(vert_out.fragPos, 1.0);
    gl_Position = combined * model * mesh * vec4(a_Position, 1.0);
}

#endif

#ifdef FRAGMENT_SHADER

#define LIMIT_ATTENUATION true

#define NR_DIRECTIONAL_LIGHTS 1
#define NR_POINT_LIGHTS 2

#define COLOUR vec4(0.5, 0.5, 0.5, 1.0)

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

in Data {
    vec3 normal;
    vec3 fragPos;
    vec4 lightSpaceFragPos;
} vert_out;

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

uniform vec3 viewPosition;

uniform bool blinnPhong;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform bool hasShadowMap;
uniform sampler2DShadow shadowMap;
uniform bool hasPointShadowMaps;
uniform samplerCubeArrayShadow pointShadowMaps;

uniform float pointShadowFarPlane;

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);
vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);

vec4 getAmbient(vec3 lightAmbient);
vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular);

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection);
float getInPointShadow(int index, vec3 fragToLight);

void main()
{
    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        float inShadow = getInShadow(vert_out.lightSpaceFragPos, globalLights[i].direction);
        vec4 dirLight = getDirLight(globalLights[i], vert_out.normal, vert_out.fragPos, viewPosition, inShadow);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        float inShadow = getInPointShadow(i, vert_out.fragPos - lights[i].position);
        vec4 pointLight = getPointLight(lights[i], vert_out.normal, vert_out.fragPos, viewPosition, inShadow);
        combinedLight += pointLight;
    }

    fragColour = combinedLight;
    bloomColour = vec4(0.0, 0.0, 0.0, 1.0);
}

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, lightDirection, normal, fragPosition, vec3(0.0), viewPosition, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec4 ambient = getAmbient(light.ambient);
    vec4 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec4 specular = getSpecular(lightDirection, light.position, normal, fragPosition, fragPosition, viewPosition, light.specular);

    float distance = length(light.position - fragPosition);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    //    return vec4((ambient + inShadow * (diffuse + specular)).rgb * attenuation, 1.0);
    return vec4(0.2, 0.2, 0.2, 1.0);
}

vec4 getAmbient(vec3 lightAmbient)
{
    return vec4(lightAmbient, 1.0) * COLOUR;
}

vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff;
    return diffuse * COLOUR;
}

vec4 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular)
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

    float specularIntensity = pow(max(specularFactor, 0.0), 64);
    vec3 specular = lightSpecular * specularIntensity;
    return vec4(specular, 1.0);
}

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection) {
    if (!hasShadowMap) {
        return 1.0;
    }

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
    if (!hasPointShadowMaps) {
        return 1.0;
    }

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

#endif
