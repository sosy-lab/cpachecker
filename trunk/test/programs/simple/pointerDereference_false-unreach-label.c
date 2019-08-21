extern void __VERIFIER_error();
int main() {
    int a = 0;

    int *p = &a;
    *p = 5;

    if (a > 0) {
ERROR:
        __VERIFIER_error();
    }
}
