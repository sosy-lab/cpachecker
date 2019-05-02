extern void __VERIFIER_error();

int * returnPointer(int * p) {
    return p;
}

int main() {
    int a = 0;

    int *p = returnPointer(&a);
    *p = 5;

    if (a > 0) {
ERROR:
        __VERIFIER_error();
    }
}
