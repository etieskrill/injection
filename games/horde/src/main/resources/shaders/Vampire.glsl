#version 330 core

varying Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoords;
    vec3 fragPos;
} vertex;

#ifdef VERTEX_SHADER

#define MAX_BONES 100
#define MAX_BONE_INFLUENCES 4

layout (location = 0) in vec3 a_Position;
layout (location = 1) in vec3 a_Normal;
layout (location = 2) in vec2 a_TexCoords;
layout (location = 3) in vec3 a_Tangent;
layout (location = 4) in vec3 a_BiTangent;
layout (location = 5) in ivec4 a_BoneIds;
layout (location = 6) in vec4 a_BoneWeights;

uniform mat4 model;
uniform mat3 normal;
uniform mat4 combined;

uniform mat4 boneMatrices[MAX_BONES];

uniform bool normalMapped;

void main()
{
    vec4 bonedPosition = vec4(0.0);

    vec3 bonedNormal = vec3(0.0);
    vec3 bonedTangent = vec3(0.0);
    vec3 bonedBiTangent = vec3(0.0);

    int bones = 0;
    for (int i = 0; i < MAX_BONE_INFLUENCES; i++) {
        if (a_BoneIds[i] == -1) break; //end of bone list
        if (a_BoneIds[i] >= MAX_BONES) break; //bones contain invalid data -> vertex is not animated
        if (a_BoneWeights[i] <= 0.0) break; //either bone has an unset weight if negative, or no influence at all

        bones++;

        vec4 localPosition = boneMatrices[a_BoneIds[i]] * vec4(a_Position, 1.0);
        bonedPosition += localPosition * a_BoneWeights[i];

        vec3 localNormal = mat3(boneMatrices[a_BoneIds[i]]) * a_Normal;
        bonedNormal += localNormal * a_BoneWeights[i];
        if (normalMapped) {
            vec3 localTangent = mat3(boneMatrices[a_BoneIds[i]]) * a_Tangent;
            bonedTangent += localTangent * a_BoneWeights[i];
            vec3 localBiTangent = mat3(boneMatrices[a_BoneIds[i]]) * a_BiTangent;
            bonedBiTangent += localBiTangent * a_BoneWeights[i];
        }
    }

    if (bones == 0) { //no bones are set, thus vertex is not involved in animation
                      bonedPosition = vec4(a_Position, 1.0);
                      bonedNormal = a_Normal;
    }

    vertex.normal = normalize(normal * bonedNormal);
    if (normalMapped) {
        vec3 tangent = normalize(normal * bonedTangent);
        vec3 biTangent = normalize(normal * bonedBiTangent);
        vertex.tbn = mat3(tangent, biTangent, vertex.normal);
    }

    vertex.texCoords = a_TexCoords;
    vertex.fragPos = vec3(model * bonedPosition);
    gl_Position = combined * model * bonedPosition;
}

#endif

#ifdef FRAGMENT_SHADER

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
    sampler2D metalness0;
    float alpha;
} material;

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 viewPosition;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

vec3 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);
vec3 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);

vec3 getAmbient(vec3 lightAmbient);
vec3 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec3 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular);

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
        vec3 dirLight = getDirLight(globalLights[i], normal, vertex.fragPos, viewPosition, 1);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec3 pointLight = getPointLight(lights[i], normal, vertex.fragPos, viewPosition, 1);
        combinedLight += pointLight;
    }

    if (material.emissiveTexture) {
        vec3 emission = texture(material.emissive0, vertex.texCoords).rgb;
        combinedLight += emission * emission;
    }

    fragColour = vec4(combinedLight, texel.a * material.alpha);

    float brightness = dot(combinedLight.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = vec4(combinedLight, texel.a * material.alpha);
    else bloomColour = vec4(0.0, 0.0, 0.0, texel.a * material.alpha);
}

vec3 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(-light.direction);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    return ambient + inShadow * (diffuse + specular);
}

vec3 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec3 ambient = getAmbient(light.ambient);
    vec3 diffuse = getDiffuse(lightDirection, normal, light.diffuse);
    vec3 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    float distance = length(light.position - fragPosition);
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
    vec3 diffuse = lightDiffuse * diff;
    return diffuse * texture(material.diffuse0, vertex.texCoords).rgb;
}

vec3 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularFactor;

    vec3 halfway = normalize(lightDirection + viewDirection);
    specularFactor = max(dot(normal, halfway), 0.0);

    float shininess = material.shininess > 0 ? material.shininess : 64;
    float specularIntensity = material.specularity * pow(specularFactor, shininess);
    vec3 specular = specularIntensity * lightSpecular;
    if (material.specularTexture) specular *= texture(material.specular0, vertex.texCoords).rgb;
    return specular;
}

#endif
