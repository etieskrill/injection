float2 applyContainerConstraint(float2 pars, float rad, float2 ws);
float2 solveCollisionsForce(global float8* pars, const int num, const int gid, float2 parPos, float rad);
float2 solveCollisionsSweep(global float8* pars, const int num, const int gid, float2 parPos, float rad);

kernel void integrate(global float8* pars, const int num, const float delta, const float2 ws) {
    const int gid = get_global_id(0);
    float8 par = pars[gid];

    float2 parPos = (float2)(par.x, par.y);
    float2 parPosPrev = (float2)(par.z, par.w);
    float2 acc = (float2)(0, 0);//(par.hi, par.lo);
    float rad = 3; //= par.even.z; //TODO replace with actual value (why tf do these specs return float4s)

    float2 grav = (float2)(0, 600);

    acc += grav;

    parPos = applyContainerConstraint(parPos, rad, ws);
    parPos = solveCollisionsForce(pars, num, gid, parPos, rad);

    float2 vel = parPos - parPosPrev;
    parPosPrev = parPos;
    parPos = parPos + vel + acc * delta * delta;

    pars[gid] = (float8)(parPos, parPosPrev, acc, 0, 0);
}

float2 applyContainerConstraint(float2 pos, float rad, float2 ws) {
    if (pos.x > ws.x - rad) {
        pos.x = ws.x - rad;
    } else if (pos.x < rad) {
        pos.x = rad;
    }

    if (pos.y > ws.y - rad) {
        pos.y = ws.y - rad;
    } else if (pos.y < rad) {
        pos.y = rad;
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
            float correct = (desiredDist - dist) / 2;
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
            if (minA > maxB) continue; //
            //TODO narrow phase
            solveCollision
        }
    }
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
