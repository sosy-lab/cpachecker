extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

int main() {
    int a = __VERIFIER_nondet_int();
    int b = a + 5;

    if (a > 0) {
        a = a - 1;
    }

    if (b > -4) {
ERROR:
        __VERIFIER_error();
        return -1;
    }
}
