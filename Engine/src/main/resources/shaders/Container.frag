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

uniform vec3 uViewPosition;
uniform vec3 uViewDirection;
uniform float uTime;

uniform DirectionalLight globalLights[NR_DIRECTIONAL_LIGHTS];
uniform PointLight lights[NR_POINT_LIGHTS];
uniform SpotLight flashlight;
uniform Material material;

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);
vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition);

void main()
{
    //Flashlight
    vec3 flashlightPart = vec3(0);

    vec3 flashlightDirection = normalize(vec3(-flashlight.position.xy, flashlight.position.z) - tFragPos);
    float theta = dot(flashlightDirection, normalize(vec3(flashlight.direction.xy, -flashlight.direction.z)));
    if (theta > flashlight.cutoff) {
        /*vec3 flashAmbient = flashlight.ambient * texture(material.diffuse, tTextureCoords).rgb;

        vec3 lightDirection = normalize(flashlight.position - tFragPos);
        float diff = max(dot(tNormal, lightDirection), 0.0); //TODO put in vertex
        vec3 flashDiffuse = flashlight.diffuse * diff * texture(material.diffuse, tTextureCoords).rgb;

        vec3 viewDirection = normalize(-uViewPosition - tFragPos);
        vec3 reflectionDirection = reflect(-lightDirection, tNormal);
        float spec = pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
        vec3 flashSpecular = flashlight.specular * spec * texture(material.specular, tTextureCoords).rgb;

        float distance = 1.0 / length(normalize(flashlight.position - tFragPos));
        //float flashAttenuation = 1.0 / flashlight.constant + flashlight.linear * distance + flashlight.quadratic * distance * distance;
        //flashAttenuation = min(attenuation, 1.0);
        float flashAttenuation = 1.0;

        flashlightPart = (flashAmbient + flashDiffuse + flashSpecular) * flashAttenuation;*/

        //flashlightPart = vec3(1f);
    }

    vec4 combinedLight = vec4(0.0);
    for (int i = 0; i < NR_DIRECTIONAL_LIGHTS; i++) {
        vec4 dirLight = calculateDirectionalLight(globalLights[i], tNormal, tFragPos, uViewPosition);
        combinedLight += vec4(dirLight.rgb, 0);
        combinedLight.a = (combinedLight.a + dirLight.a) / 2; //TODO this FEELS correct for transparent objects
    }
    for (int i = 0; i < NR_POINT_LIGHTS; i++) {
        vec4 pointLight = calculatePointLight(lights[i], tNormal, tFragPos, uViewPosition);
        combinedLight += vec4(pointLight.rgb, pointLight.a);
        combinedLight.a = (combinedLight.a + pointLight.a) / 2;
    }

    vec4 emission = vec4(0.0);
    if (length(texture(material.specular0, tTextureCoords).rgb) == 0.0) {
        emission = texture(material.emissive0, tTextureCoords + vec2(0.0, uTime * 0.25));
        emission = emission.grba * 0.7;
    }
    //combinedLight += emission;

    oColour = combinedLight;
}

vec4 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec4 texel = texture(material.diffuse0, tTextureCoords);
    if (texel.a < 0.01) discard;

    vec3 ambient = light.ambient * texel.rgb;

    vec3 lightDirection = normalize(-light.direction);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = light.diffuse * diff * texel.rgb;

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    vec3 specular = light.specular * spec * texture(material.specular0, tTextureCoords).rgb;

    return vec4(ambient + diffuse + specular, texel.a);
}

vec4 calculatePointLight(PointLight light, vec3 normal, vec3 fragPosition, vec3 viewPosition)
{
    vec4 texel = texture(material.diffuse0, tTextureCoords);
    if (texel.a < 0.01) discard;
    vec3 ambient = light.ambient * texel.rgb;

    vec3 lightDirection = normalize(light.position - fragPosition);
    float diff = max(dot(normal, lightDirection), 0.0);
    vec3 diffuse = light.diffuse * diff * texel.rgb;

    vec3 reflectionDirection = reflect(-lightDirection, normal);
    vec3 viewDirection = normalize(viewPosition - fragPosition);
    float spec = material.specularity * pow(max(dot(viewDirection, reflectionDirection), 0.0), material.shininess);
    vec3 specular = light.specular * spec * texture(material.specular0, tTextureCoords).rgb;

    float distance = length(lightDirection);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * distance * distance);
    if (LIMIT_ATTENUATION) attenuation = min(attenuation, 1.0);

    return vec4((ambient + diffuse + specular) * attenuation, texel.a);
}
