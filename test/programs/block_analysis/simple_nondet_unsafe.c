extern int __VERIFIER_nondet_int();

int main() {

    int y = __VERIFIER_nondet_int();

    if (y < 0) {
        y = -y;
    }

    if (y > 100) {
        y = 100;
    }

    int x = -y;

    if (x * y <= 0) {
        goto ERROR;
    }
    return 0;
    ERROR: return 1;
}
