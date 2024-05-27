#version 330 core

#ifdef VERTEX_SHADER
layout (location = 0) in vec3 a_Pos;

out vec4 fragPosition;

uniform mat4 uModel;
uniform mat4 uCombined;

void main()
{
    //    fragPosition = uModel * vec4(a_Pos, 1.0);
    //    gl_Position = uCombined * fragPosition;
    gl_Position = uCombined * uModel * vec4(a_Pos, 1.0);
}
#endif

#ifdef FRAGMENT_SHADER
in vec4 fragPosition;

out vec4 fragColour;

bool isInteger(float number) {
    return ceil(number) == number;
}

void main()
{
    //    if (isInteger(fragPosition.x) || isInteger(fragPosition.y) || isInteger(fragPosition.z)) {
    fragColour = vec4(1.0, 0.0, 0.0, 1.0);
    //    }
}
#endif
