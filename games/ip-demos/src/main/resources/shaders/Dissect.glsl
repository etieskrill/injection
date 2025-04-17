#version 330 core

#pragma stage vertex

const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));

out vec2 fragCoord;

void main()
{
    gl_Position = vec4(vertices[gl_VertexID], 0, 1);
    fragCoord = vec2(gl_Position.xy);
}

#pragma stage fragment

uniform vec2 iResolution;

//#define TIME        iTime
#define RESOLUTION vec2(1920, 1080)
//iResolution
#define PI          3.141592654
#define PI_2        (0.5*PI)
#define TAU         (2.0*PI)
#define SCA(a)      vec2(sin(a), cos(a))
#define ROT(a)      mat2(cos(a), sin(a), -sin(a), cos(a))

// License: WTFPL, author: sam hocevar, found: https://stackoverflow.com/a/17897228/418488
const vec4 hsv2rgb_K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + hsv2rgb_K.xyz) * 6.0 - hsv2rgb_K.www);
    return c.z * mix(hsv2rgb_K.xxx, clamp(p - hsv2rgb_K.xxx, 0.0, 1.0), c.y);
}
// License: WTFPL, author: sam hocevar, found: https://stackoverflow.com/a/17897228/418488
//  Macro version of above to enable compile-time constants
#define HSV2RGB(c)  (c.z * mix(hsv2rgb_K.xxx, clamp(abs(fract(c.xxx + hsv2rgb_K.xyz) * 6.0 - hsv2rgb_K.www) - hsv2rgb_K.xxx, 0.0, 1.0), c.y))

// License: MIT OR CC-BY-NC-4.0, author: mercury, found: https://mercury.sexy/hg_sdf/
vec2 mod2(inout vec2 p, vec2 size) {
    vec2 c = floor((p + size*0.5)/size);
    p = mod(p + size*0.5,size) - size*0.5;
    return c;
}

// License: MIT, author: Inigo Quilez, found: https://iquilezles.org/www/articles/intersectors/intersectors.htm
float rayPlane(vec3 ro, vec3 rd, vec4 p) {
    return -(dot(ro,p.xyz)+p.w)/dot(rd,p.xyz);
}

vec3 groundRender(vec3 col, vec3 ro, vec3 rd, inout float maxt) {
    const vec3 groundPlaneNormal = normalize(vec3(0.0, 1.0, 0.0));
    const vec4 groundPlaneEquation = vec4(groundPlaneNormal, 0.0);

    //cast perspective ray in screen space
    float groundPlaneDistance = rayPlane(ro, rd, groundPlaneEquation);

    if (groundPlaneDistance < 0.0) { //ray does not intersect ground
        return col;
    }

    maxt = groundPlaneDistance;

    vec3 groundPoint = ro + rd*groundPlaneDistance; //resolve ground point by applying plane formula
    //float gpfre = 1.0 + dot(rd, groundPlaneNormal); //i presume an approximation for a fresnel term
    //gpfre *= gpfre;
    //gpfre *= gpfre;
    //gpfre *= gpfre;

    //vec3 groundRayReflected = reflect(rd, groundPlaneNormal);

    vec2 ggp    = groundPoint.xz;
    //ggp.y += TIME;
    float dfy   = dFdy(ggp.y);
    float gcf = sin(ggp.x)*sin(ggp.y);
    vec2 ggn    = mod2(ggp, vec2(1.0));
    float ggd   = min(abs(ggp.x), abs(ggp.y));

    vec3 gcol = hsv2rgb(vec3(0.7/*+0.1*gcf*/, 0.90, 0.02));

    float rmaxt = 1E6;
    //vec3 rcol = outerSkyRender(groundPoint, groundRayReflected);
    //vec3 rcol;
    //rcol = mountainRender(rcol, groundPoint, groundRayReflected, true, rmaxt);
    //rcol = triRender(rcol, groundPoint, groundRayReflected, rmaxt);

    col = (gcol / max(ggd, 0.0+0.25*dfy)) * exp(-0.25*groundPlaneDistance);
    //rcol += HSV2RGB(vec3(0.65, 0.85, 1.0))*gpfre;
    //rcol = 4.0*tanh(rcol*0.25);
    //col += rcol*gpfre;

    return col;
}

vec3 render(vec3 ro, vec3 rd) {
    float maxt = 1E6;

    //vec3 col = outerSkyRender(ro, rd);
    vec3 col;
    col = groundRender(col, ro, rd, maxt);
    //col = mountainRender(col, ro, rd, false, maxt);
    //col = triRender(col, ro, rd, maxt);

    return col;
}

vec3 effect(vec2 p, vec2 pp) {
    const float fov = tan(TAU/6.0);
    const vec3 ro = 1.0*vec3(0.0, 1.0, -4.);
    const vec3 la = vec3(0.0, 1.0, 0.0);
    const vec3 up = vec3(0.0, 1.0, 0.0);

    vec3 ww = normalize(la - ro);
    vec3 uu = normalize(cross(up, ww));
    vec3 vv = cross(ww,uu);
    vec3 rd = normalize(-p.x*uu + p.y*vv + fov*ww);

    float aa = 2.0/RESOLUTION.y;

    vec3 col = render(ro, rd);
#if defined(THAT_CRT_FEELING)
    col *= smoothstep(1.5, 0.5, length(pp));
    col *= 1.25*mix(vec3(0.5), vec3(1.0),smoothstep(-0.9, 0.9, sin(0.25*TAU*p.y/aa+TAU*vec3(0.0, 1., 2.0)/3.0)));
#endif
    //col -= 0.05*vec3(.00, 1.0, 2.0).zyx;
    //col = aces_approx(col);
    //col = sRGB(col);
    return col;
}

in vec2 fragCoord;
out vec4 fragColor;

void main() {
    //vec2 q = fragCoord/RESOLUTION.xy;
    vec2 q = fragCoord + 0.5;

    vec2 p = -1. + 2. * q;
    vec2 pp = p;
    p.x *= RESOLUTION.x/RESOLUTION.y;
    vec3 col = effect(p, pp);

    fragColor = vec4(col, 1.0);
}
