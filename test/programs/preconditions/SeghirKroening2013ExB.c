extern void __VERIFIER_error() __attribute__ ((__noreturn__));
int al;
void copy(int a[], int b[]) {
    int i;
    i = 0;
    while (b[i] != 0) {
        if (i >= al) {
            ERROR: __VERIFIER_error();
        }
        a[i] = b[i];
        i++;
    }
}
