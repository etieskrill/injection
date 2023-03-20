kernel void print(global float2* pos, const int test1, const float test2) {
    const int gid = get_global_id(0);
    pos[0] = (float2)(10, 20);
    printf("Hullo %d %v2hlf ", gid, pos[0]);
}