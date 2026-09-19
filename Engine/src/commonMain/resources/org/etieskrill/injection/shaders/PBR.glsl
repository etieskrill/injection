#version 330 core

#ifdef VERTEX_SHADER
void main()
{
}
#endif

#ifdef FRAGMENT_SHADER
varying vec3 frag_pos;
varying vec2 tex_coord;
varying mat3 tbn;

out vec4 frag_colour;
out vec4 bloom_colour;

#define NUM_DIR_LIGHTS 1
#define NUM_POINT_LIGHTS 2

uniform struct DirectionalLight {
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
} dir_lights[NUM_DIR_LIGHTS];

uniform struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
} point_lights[NUM_POINT_LIGHTS];

uniform vec3 view_pos;

uniform struct Material {
    sampler2D diffuse0;
    sampler2D normal0;
    sampler2D emission0;
} material;

vec3 get_emission(vec3 frag_pos, vec3 view_dir);
vec3 get_brd(vec3 frag_pos, vec3 light_dir, vec3 view_dir);

void main()
{
    vec3 light = vec3();

    vec3 view_dir = normalize(view_pos - frag_pos);

    light += get_emission(frag_pos, view_dir);

    vec3 normal = texture(material.normal0, tex_coord);
    normal = normalize(tbn * normal);

    for (int i = 0; i < NUM_DIR_LIGHTS; i++) {
        vec3 light_intensity = dir_lights[i].diffuse;
        vec3 light_dir = normalize(dir_lights[i].direction);
        light += get_brd(frag_pos, light_dir, view_dir) * light_intensity * dot(light_dir, normal);
    }
    for (int i = 0; i < NUM_POINT_LIGHTS; i++) {
        vec3 light_intensity = point_lights[i].diffuse;
        vec3 light_dir = normalize(point_lights[i].position - view_pos);
        light += get_brd(frag_pos, light_dir, view_dir) * light_intensity * dot(light_dir, normal);
    }

    frag_colour = light;
}

vec3 get_emission(vec3 frag_pos, vec3 view_dir)
{
    //TODO figure out whatever the arguments are supposed to be used for
    return texture(material.emission0, tex_coord);
}

vec3 get_brd(vec3 frag_pos, vec3 light_dir, vec3 view_dir)
{
}

vec3 get_fresnel_factor()
{
    vec3 F0 = mix(vec3(0.04), material.diffuse0);
}
#endif
