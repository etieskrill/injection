#version 330 core

out vec4 oColour;

struct Light {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform Light light;

void main() {

    oColour = vec4(light.ambient + light.diffuse + light.specular, 1.0);

}
