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
    sampler2D normal0;
    bool specularTexture;
    float shininess;
    float specularity;
    sampler2D emissive0;
    samplerCube cubemap0;
};

in Data {
    vec3 normal;
    mat3 tbn;
    vec2 texCoord;
    vec3 fragPos;
    vec4 lightSpaceFragPos;
} vert_out;

out vec4 fragColour;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 uViewPosition;
uniform mat3 uNormal;

uniform bool uNormalMapped;
uniform bool uBlinnPhong;

uniform Material material;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

uniform sampler2DShadow u_ShadowMap;

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);
vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition, float inShadow);

vec4 getAmbient(vec3 lightAmbient);
vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular);

vec4 getCubeReflection(vec3 normal);
vec4 getCubeRefraction(float refractIndex, vec3 normal);

float getInShadow(vec4 lightSpaceFragPos, vec3 lightDirection);

void main()
{
    vec3 normal;
    if (uNormalMapped) {
        normal = texture(material.normal0, vert_out.texCoord).xyz * 2.0 - 1.0;
        normal = normalize(normal);
        normal = normalize(vert_out.tbn * normal);
    } else {
        normal = vert_out.normal;
    }

    vec4 texel = texture(material.diffuse0, vert_out.texCoord);
    if (texel.a == 0.0) discard;

    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        float inShadow = getInShadow(vert_out.lightSpaceFragPos, globalLights[i].direction);
        vec4 dirLight = getDirLight(globalLights[i], normal, vert_out.fragPos, uViewPosition, inShadow);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec4 pointLight = getPointLight(lights[i], normal, vert_out.fragPos, uViewPosition, 1.0);
        combinedLight += pointLight;
    }

    //    vec4 emission = texture(material.emissive0, vert_out.texCoord);
    //    combinedLight += vec4(emission.rgb, 0);

    //TODO pack reflection mix factor into material property. until reflection maps are a thing, anyway
    //    combinedLight += getCubeReflection(normal);

    //    if (combinedLight.a < 0.9) {
    //        combinedLight = mix(combinedLight, getCubeRefraction(1 / 1.52, normal), 0.5);
    //    }

    fragColour = combinedLight;
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

    return vec4((ambient + inShadow * (diffuse + specular)).rgb * attenuation, 1.0);
}

vec4 getAmbient(vec3 lightAmbient)
{
    vec4 ambient = vec4(lightAmbient, 1.0);
    return ambient * texture(material.diffuse0, vert_out.texCoord);
}

vec4 getDiffuse(vec3 lightDirection, vec3 normal, vec3 lightDiffuse)
{
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff;
    return diffuse * texture(material.diffuse0, vert_out.texCoord);
}

vec4 getSpecular(vec3 lightDirection, vec3 lightPosition, vec3 normal, vec3 fragPosition, vec3 lightDirFragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float specularFactor;
    if (uBlinnPhong) {
        vec3 lightDirection = normalize(lightPosition - lightDirFragPosition);
        vec3 halfway = normalize(lightDirection + viewDirection);
        specularFactor = dot(normal, halfway);
    } else {
        vec3 reflectionDirection = reflect(-lightDirection, normal);
        specularFactor = dot(viewDirection, reflectionDirection);
    }

    float specularIntensity = material.specularity * pow(clamp(specularFactor, 0.0, 1.0), material.shininess);
    vec4 specular = vec4(lightSpecular, 1.0) * specularIntensity;
    if (material.specularTexture) specular *= texture(material.specular0, vert_out.texCoord);
    return specular;
}

vec4 getCubeReflection(vec3 normal) {
    vec3 viewDirection = normalize(vert_out.fragPos - uViewPosition);
    vec3 viewReflection = reflect(viewDirection, normal);
    return texture(material.cubemap0, -viewReflection);
}

vec4 getCubeRefraction(float refractIndex, vec3 normal) {
    vec3 viewDirection = normalize(vert_out.fragPos - uViewPosition);
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

    vec3 texelSize = vec3(1.0 / textureSize(u_ShadowMap, 0), 1.0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            shadow += texture(u_ShadowMap, depthSpace + vec3(x, y, 0.0) * texelSize);
        }
    }

    return shadow / 9.0;
}
