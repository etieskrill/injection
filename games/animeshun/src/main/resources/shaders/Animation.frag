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
    float shininess;
    float specularity;
    sampler2D emissive0;
    samplerCube cubemap0;
};

out vec4 oColour;

in vec3 tNormal;
in vec2 tTextureCoords;
in vec3 tFragPos;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 uViewPosition;

uniform Material material;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 getAmbientAndDiffuse(vec3 lightDirection, vec3 normal, vec3 lightAmbient, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular);
vec4 getCubeReflection();
vec4 getCubeRefraction(float refractIndex);

void main()
{
    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        vec4 dirLight = calculateDirectionalLight(globalLights[i], tNormal, tFragPos, uViewPosition);
        //        combinedLight = mix(combinedLight, dirLight, 0.5);
        combinedLight += dirLight;
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec4 pointLight = calculatePointLight(lights[i], tNormal, tFragPos, uViewPosition);
        combinedLight = mix(combinedLight, pointLight, 0.5);
    }

    vec4 emission = vec4(0.0);
    //    if (length(texture(material.specular0, tTextureCoords).rgb) == 0.0) //sometimes causes weird artifacts, and tbh, i do not remember what this was for
    emission = texture(material.emissive0, tTextureCoords);
    combinedLight += vec4(emission.rgb, 0);

    //TODO pack reflection mix factor into material property. until reflection maps are a thing, anyway
    combinedLight = mix(combinedLight, getCubeReflection(), 0.2);

    //    if (combinedLight.a < 0.9) {
    //        combinedLight = mix(combinedLight, getCubeRefraction(1 / 1.52), 0.5);
    //    }

    oColour = combinedLight;
}

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 lightDirection = normalize(-light.direction);
    vec4 ambientAndDiffuse = getAmbientAndDiffuse(lightDirection, normal, light.ambient, light.diffuse);

    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    return ambientAndDiffuse + specular;
}

vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec4 ambientAndDiffuse = getAmbientAndDiffuse(lightDirection, normal, light.ambient, light.diffuse);

    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    float distance = length(lightDirection);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return (ambientAndDiffuse + specular) * attenuation;
}

vec4 getAmbientAndDiffuse(vec3 lightDirection, vec3 normal, vec3 lightAmbient, vec3 lightDiffuse)
{
    vec4 texel = texture(material.diffuse0, tTextureCoords);
    if (texel.a < 0.01) discard;

    vec4 ambient = vec4(lightAmbient, 1.0) * texel;

    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff * texel;

    return ambient + diffuse;
}

vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    return vec4(lightSpecular, 1.0) * spec * texture(material.specular0, tTextureCoords);
}

vec4 getCubeReflection() {
    vec3 viewDirection = normalize(tFragPos - uViewPosition);
    vec3 viewReflection = reflect(viewDirection, tNormal);
    return texture(material.cubemap0, -viewReflection);
}

vec4 getCubeRefraction(float refractIndex) {
    vec3 viewDirection = normalize(tFragPos - uViewPosition);
    vec3 viewRefraction = refract(viewDirection, tNormal, refractIndex);
    return texture(material.cubemap0, -viewRefraction);
}
