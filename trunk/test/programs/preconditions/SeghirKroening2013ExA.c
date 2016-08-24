extern void __VERIFIER_error() __attribute__ ((__noreturn__));
int al;
void copy(int a[], int b[], int bl) {
    int i;
    i = 0;
    while (i < bl) {
        if (i >= al) {
            ERROR: __VERIFIER_error();
        }
        a[i] = b[i];
        i++;
    }
}
