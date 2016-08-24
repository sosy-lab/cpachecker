int foo(int p, int x, int z) {
    if (z < 0 || x < 0) {
        return 0;
    }

    int i = x;
    if (p == 1) {
       i = i + z; 
        if (i > 65535) {
            goto ERROR;
        }
    } else {
       i = i + z + 1; 
       if (i > 255) {
            goto ERROR;
       }
    }

    return i;
    ERROR: return -1;
}
