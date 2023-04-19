float2 applyContainerConstraint(float2 pars, float rad, float2 wSize, float2 wPos);
float2 solveCollisionsForce(global float8* pars, const int num, const int gid, float2 parPos, float rad);
float2 solveCollisionsSweep(global float8* pars, const int num, const int gid, float2 parPos, float rad);

kernel void integrate(global float8* particles, const int num, const float delta, const float2 wSize, const float2 wPos) {
    const int gid = get_global_id(0);
    float8 particle = particles[gid];

    float2 position = (float2)(particle.x, particle.y);
    float2 previousPosition = (float2)(particle.z, particle.w);
    float2 acceleration = (float2)(0, 0);//(particle.hi, particle.lo);
    float radius = 3; //= particle.even.z; //TODO replace with actual value (why tf do these specs return float4s)

    float2 gravity = (float2)(0, 1000);

    acceleration += gravity;

    position = applyContainerConstraint(position, radius, wSize, wPos);
    position = solveCollisionsForce(particles, num, gid, position, radius);

    //float damping = 0.5;

    //float2 vel = (position - previousPosition) * (float)(1 - (damping * delta));
    float2 vel = position - previousPosition;
    previousPosition = position;
    position = position + vel + acceleration * delta * delta;
    //position -= 0.001f * (position - previousPosition);

    particles[gid] = (float8)(position, previousPosition, acceleration, 0, 0);
}

float2 applyContainerConstraint(float2 pos, float rad, float2 wSize, float2 wPos) {
    //pos.x + rad > wPos.x + wSize.x

    if (pos.x > wPos.x + wSize.x - rad) {
        pos.x -= (pos.x - (wPos.x + wSize.x - rad)) * 0.025f;
    } else if (pos.x < wPos.x + rad) {
        pos.x -= (pos.x - (wPos.x + rad)) * 0.025f;
    }

    if (pos.y > wPos.y + wSize.y - rad) {
        pos.y -= (pos.y - (wPos.y + wSize.y - rad)) * 0.025f;
    } else if (pos.y < wPos.y + rad) {
        pos.y -= (pos.y - (wPos.y + rad)) * 0.025f;
    }

    return pos;
}

float2 solveCollisionsForce(global float8* pars, const int num, const int gid, float2 parPos, float rad) {
    for (int i = 0; i < num; i++) {
        if (i == gid) continue;
        float8 par2 = pars[i];
        float2 relPos = parPos - (float2)(par2.x, par2.y);
        float dist = length(relPos);
        float desiredDist = 3 + rad; //TODO fill in radius
        if (dist < desiredDist) {
            float2 normalPos = normalize(relPos);
            float correct = (desiredDist - dist) / 8; //TODO originally 2, is this a bad idea?
            normalPos *= correct;
            parPos += normalPos;
            pars[i].x -= normalPos.x;
            pars[i].y -= normalPos.y;
        }
    }

    return parPos;
}

void findCollisions(
    global float8* particles,
    global int* sortedParticles,
    const int numParticles,
    const int forY,
    global int2* collisions,
    int* numCollisions
    )
{
    int numCols = 0;
    for (int i = 0; i < numParticles; i++) {
        float posA = forY ? particles[sortedParticles[i]].y : particles[sortedParticles[i]].x;
        float radA = 3;//particles[sortedParticles[i]]. TODO adjust
        float minA = posA - radA;
        float maxA = posA + radA;
        for (int j = i; j < numParticles; j++) {
            float posB = forY ? particles[sortedParticles[j]].y : particles[sortedParticles[j]].x;
            float radB = 3; //TODO adjust
            float minB = posB - radB;
            float maxB = posB + radB;
            if (maxA < minB) break; //Searching particle's span ended
            if (minA > maxB) continue; //TODO This should never happen though???

            //At this point, the two boundary boxes are colliding on this axis
            collisions[numCols] = (int2)(i, j);
            numCols++;
        }
    }

    *numCollisions = numCols;
}

float2 solveCollisionsSweep(global float8* pars, const int num, const int gid, float2 parPos, float rad) {
    return (float2)(0, 0);
}

//Single kernel array traversal and insertion
kernel void sort(global float8* particles, const int num, global int* sorted, const int forY) {
    for (int i = 0; i < num; i++) {
        printf("%v8hlf\n", particles[i]);
        if (sorted + i == NULL) sorted[i] = i;
        for (int j = i; j > 0; j--) {
            float v1 = forY ? particles[sorted[j]].y : particles[sorted[j]].x;
            float v2 = forY ? particles[sorted[j - 1]].y : particles[sorted[j - 1]].x;
            printf("%f, %f\n", v1, v2);
            if (v1 > v2) {
                int tmp = sorted[j];
                sorted[j] = sorted[j - 1];
                sorted[j - 1] = tmp;
            }
            //eod im bloody tired, i clogged the toilet and i am going to bed
        }
    }
}

//Multi-kernel integrity traversal

//Single kernel array insertion //TODO maybe rough lookup table?
