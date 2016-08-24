int main() {

    int x, y, z;
    int *p, *q;
    int **r;

    y = 42;
    x = y;
    q = malloc(x);
    p = &x;
    r = &p;
    q = &y;
    *r = q;
    r = &q;

    if (x == 42) {
        p = &z;
    }

    return (0);
}

