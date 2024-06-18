#version 330 core

#ifdef VERTEX_SHADER
layout (location = 0) in vec3 a_Position;
layout (location = 2) in vec2 a_TexCoords;

out vec3 fragPosition;
out vec2 texCoords;

uniform mat4 uModel;
uniform mat4 uCombined;

void main() {
    gl_Position = uCombined * uModel * vec4(a_Position, 1.0);
    fragPosition = vec3(uModel * vec4(a_Position, 1.0));
    texCoords = a_TexCoords;
}
#endif

#ifdef FRAGMENT_SHADER
in vec3 fragPosition;
in vec2 texCoords;

out vec4 fragColour;

bool isInside(float number, float width) {
    return number < width / 2 && number > -width / 2;
}

bool isInsideMod(float number, float width) {
    float modNumber = mod(number, 1);
    return modNumber < width / 2 || modNumber > (1 - width / 2);
}

void main() {
    if (fragPosition.y != 0) {
        discard;
    }

    float cardinalWidth = 0.02;
    float width = 0.01;
    if (isInside(fragPosition.y, cardinalWidth) && isInside(fragPosition.z, cardinalWidth)) {
        fragColour = vec4(0.3, 0.0, 0.0, 1.0);
    } else if (isInside(fragPosition.x, cardinalWidth) && isInside(fragPosition.z, cardinalWidth)) {
        fragColour = vec4(0.0, 0.3, 0.0, 1.0);
    } else if (isInside(fragPosition.x, cardinalWidth) && isInside(fragPosition.y, cardinalWidth)) {
        fragColour = vec4(0.0, 0.0, 0.3, 1.0);
    } else if (isInsideMod(texCoords.x, width) || isInsideMod(texCoords.y, width)) {
        fragColour = vec4(0.2, 0.2, 0.2, 1.0);
    } else {
        discard;
    }
}
#endif
