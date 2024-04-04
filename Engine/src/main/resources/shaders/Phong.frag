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
    float shininess;
    float specularity;
    sampler2D emissive0;
    samplerCube cubemap0;
};

in Data {
    mat3 tbn;
    vec3 normal;
    vec2 texCoord;
    vec3 fragPos;
} vert_out;

out vec4 fragColour;

//TODO implement this in view space to mitigate passing such variables anyway
uniform vec3 uViewPosition;
uniform mat3 uNormal;

uniform Material material;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 getAmbientAndDiffuse(vec3 lightDirection, vec3 normal, vec3 lightAmbient, vec3 lightDiffuse);
vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular);
vec4 getCubeReflection(vec3 normal);
vec4 getCubeRefraction(float refractIndex, vec3 normal);

void main()
{
    vec3 normal = normalize(texture(material.normal0, vert_out.texCoord).xyz);
////    normal = (normal * 2.0) - 1.0;
    normal = normalize(vert_out.tbn * normal);
//    vec3 normal = vert_out.normal;

    fragColour = vec4(0.0);
    fragColour += getDirLight(globalLights[0], normal, vert_out.fragPos, uViewPosition);
//    fragColour += getPointLight(lights[0], normal, vert_out.fragPos, uViewPosition);

//    vec4 texel = texture(material.diffuse0, vert_out.texCoord);
//    if (texel.a == 0.0) discard;
//
//    vec4 combinedLight = vec4(0.0);
//    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
//        vec4 dirLight = getDirLight(globalLights[i], normal, vert_out.fragPos, uViewPosition);
//        combinedLight += dirLight;
//    }
//    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
//        vec4 pointLight = getPointLight(lights[i], normal, vert_out.fragPos, uViewPosition);
//        combinedLight += pointLight;
//    }

//    combinedLight = vec4(1.0 - abs(dot(vert_out.normal, vert_out.tbn * (texture(material.normal0, vert_out.texCoord).rgb * 2.0 - 1.0))), 0.0, 0.0, 1.0);

//    vec4 emission = vec4(0.0);
    //    if (length(texture(material.specular0, tTextureCoords).rgb) == 0.0) //sometimes causes weird artifacts, and tbh, i do not remember what this was for
//    emission = texture(material.emissive0, vert_out.texCoord);
    //    combinedLight += vec4(emission.rgb, 0);

    //TODO pack reflection mix factor into material property. until reflection maps are a thing, anyway
//    combinedLight += getCubeReflection(normal);

//    if (combinedLight.a < 0.9) {
//        combinedLight = mix(combinedLight, getCubeRefraction(1 / 1.52, normal), 0.5);
//    }

//    fragColour = combinedLight;
}

vec4 getDirLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 lightDirection = normalize(-light.direction);
    vec4 ambientAndDiffuse = getAmbientAndDiffuse(lightDirection, normal, light.ambient, light.diffuse);

    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    return ambientAndDiffuse + specular;
}

vec4 getPointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec3 lightDirection = normalize(light.position - fragPosition);
    vec4 ambientAndDiffuse = getAmbientAndDiffuse(lightDirection, normal, light.ambient, light.diffuse);

    vec4 specular = getSpecular(lightDirection, normal, fragPosition, viewPosition, light.specular);

    float distance = length(light.position - fragPosition);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

//    return (ambientAndDiffuse + specular) * attenuation;
    return (ambientAndDiffuse * attenuation) + specular;
}

vec4 getAmbientAndDiffuse(vec3 lightDirection, vec3 normal, vec3 lightAmbient, vec3 lightDiffuse)
{
    vec4 ambient = vec4(lightAmbient, 1.0);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec4 diffuse = vec4(lightDiffuse, 1.0) * diff;
    return (ambient + diffuse) * texture(material.diffuse0, vert_out.texCoord);
}

vec4 getSpecular(vec3 lightDirection, vec3 normal, vec3 fragPosition, vec3 viewPosition, vec3 lightSpecular)
{
    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    return vec4(lightSpecular, 1.0) * spec * texture(material.specular0, vert_out.texCoord);
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
