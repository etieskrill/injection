//copied here so the glsl plugin can link to it

struct Camera {
    mat4 perspective;
    mat4 combined;
    vec3 position;
    float near;
    float far;
    ivec2 viewport;
    float aspect;
};

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
