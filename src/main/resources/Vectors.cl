kernel void print(float2* pos, const int test1, const float test2) {
    //const int gid = get_global_id(0);
    pos[0] = 10;
    pos[1] = 20;
    printf("Hullo");
}