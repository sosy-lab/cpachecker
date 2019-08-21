extern void __VERIFIER_error();
int main() {
    int a = 0;

    int *p = &a;
    a = 5;

    if (*p > 0) {
ERROR:
        __VERIFIER_error();
    }
}
