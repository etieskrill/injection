float2 applyContainerConstraint(float2 pos, float rad, float2 ws);
float2 solveCollisions(float8* pos, const int num, const int gid, float2 parPos, float rad);

kernel void integrate(global float8* pos, const int num, const float delta, const float2 ws) {
    const int gid = get_global_id(0);
    float8 par = pos[gid];

    float2 parPos = (float2)(par.x, par.y);
    float2 parPosPrev = (float2)(par.z, par.w);
    float2 acc = (float2)(0, 0);//(par.hi, par.lo);
    float rad = 3; //= par.even.z; //TODO replace with actual value (why tf do these specs return float4s)

    float2 grav = (float2)(0, 600);

    acc += grav;

    parPos = applyContainerConstraint(parPos, rad, ws);
    parPos = solveCollisions(pos, num, gid, parPos, rad);

    float2 vel = parPos - parPosPrev;
    parPosPrev = parPos;
    parPos = parPos + vel + acc * delta * delta;

    pos[gid] = (float8)(parPos, parPosPrev, acc, 0, 0);
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

float2 solveCollisions(float8* pos, const int num, const int gid, float2 parPos, float rad) {
    for (int i = 0; i < num; i++) {
        if (i == gid) continue;
        float8 par2 = pos[i];
        float2 relPos = parPos - (float2)(par2.x, par2.y);
        float dist = length(relPos);
        float desiredDist = 3 + rad; //TODO fill in radius
        if (dist < desiredDist) {
            float2 normalPos = normalize(relPos);
            float correct = (desiredDist - dist) / 2;
            normalPos *= correct;
            parPos += normalPos;
            pos[i].x -= normalPos.x;
            pos[i].y -= normalPos.y;
        }
    }

    return parPos;
}

//Single kernel array traversal and insertion
kernel void sort(global float8* particles, const int num, global float8** sorted, const int forY) {
    for (int i = 0; i < num; i++) {
        if (sorted[i] == NULL) sorted[i] = particles + i;
        for (int j = i; j > 0; j--) {
            float v1 = forY ? (*sorted[j]).y : (*sorted[j]).x;
            float v2 = forY ? (*sorted[j - 1]).y : (*sorted[j - 1]).x;
            if (v1 > v2) {
                global float8* tmp = sorted[j];
                sorted[j] = sorted[j - 1];
                sorted[j - 1] = tmp;
            }
            //eod im bloody tired, i clogged the toilet and i am going to bed
        }
    }
}

//Multi-kernel integrity traversal

//Single kernel array insertion //TODO maybe rough lookup table?
