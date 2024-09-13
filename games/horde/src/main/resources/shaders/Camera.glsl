struct Camera {
    mat4 perspective;
    mat4 combined;
    vec3 position;
    float near;
    float far;
    ivec2 viewport;
    float aspect;
};
