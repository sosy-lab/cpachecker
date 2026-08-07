extern unsigned __VERIFIER_nondet_uint();
extern void __VERIFIER_assume(int);
extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
    unsigned size = __VERIFIER_nondet_uint(); // or a big number like size = 1000000;
    __VERIFIER_assume(size >= 11);

    int A[size];

    int i = 0;
        while (i < size) {
            A[i] = 42;
            i++;
        }

    if ((A[10]!=42)){
        __assert_fail("0", "array_init.c", 23, "main");
    }
}

//Current Verification result: UNKNOWN with 900s limit (TRUE if size is quite small)