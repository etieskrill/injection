#version 330 core

layout (location = 0) out vec4 fragColour;
layout (location = 1) out vec4 bloomColour;

struct Light {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform Light light;

void main()
{
    fragColour = vec4(light.diffuse, 1.0);

    float brightness = dot(light.diffuse, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > 1.0) bloomColour = fragColour;
    else bloomColour = vec4(0.0, 0.0, 0.0, 1.0);
}
