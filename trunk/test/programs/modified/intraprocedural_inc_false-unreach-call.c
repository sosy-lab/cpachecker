extern void __VERIFIER_error();
extern int __VERIFIER_nondet_int();

int main() {
    int a = __VERIFIER_nondet_int();

    if (a <= 0) {
        a = -1;

        if (a < 0) {
ERROR:
            __VERIFIER_error();
        }
    } else {
        int i = 0;
        while (i < 1000000) {
            a = a + 1;
            i = i + 1;
        }

        // if a > 0 at the beginning of above loop,
        // + 1000000 is not sufficient to reach 0
        if (i < 1000000 || a == 0) {
            goto ERROR;
        }

    }
}
