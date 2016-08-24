int foo(int p, int x) {
    if (x < 0) {
        return 0;
    }

    if (p == 1) {
        if (x > 65535) {
            goto ERROR;
        }
    }

    return x;
    ERROR: return -1;
}
