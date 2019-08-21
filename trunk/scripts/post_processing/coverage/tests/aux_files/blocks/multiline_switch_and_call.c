int __VERIFIER_nondet_int();
int cond(int x1, int x2, int x3, int x4) {
    return x1 &&
        x2 && x3 && x4;
}
int main() {
    int i = __VERIFIER_nondet_int();
    int v1 = 0;
    int v2 = 0;
    int v3 = 0;
    int v4 = 0;
    if (i > -100 && i < 100 && i*i < 0) {
        v1 = 1;
        v2 = 1;
        v3 = 1;
        v4 = 1;
    }
    switch (cond(v1,
            v2,
            v3,
            v4)) {
        case 1:
            i = i + 1;
            i = i + 2;
            i = i + 3;
            break;
        case 0:
            i = i + 4;
            break;
        default:
            i = i + 5;
            i = i + 6;
            i = i + 7;
            break;
    }
}
