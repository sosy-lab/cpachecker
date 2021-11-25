extern int __VERIFIER_nondet_int();

int main() {
    int x = __VERIFIER_nondet_int();
    if (x <= 0) {
        x = 100;
    }
    if (x > 100) {
        x = 100;
    }
    int y = 10;
    if (x + y == 10) {
        y = y - x;
    } else {
        y = y + x;
    }
    if (y != 10) {
        goto ERROR;
    }
    return 0;
ERROR: return 1;
}
