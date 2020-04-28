int main() {
    int n=__VERIFIER_nondet_int();
    __VERIFIER_assume(0 < n);
    int a[n];

    int i=__VERIFIER_nondet_int();
    __VERIFIER_assume(0 <= i && i < n);

    int i0 = i;
    for(; i<n; i++) {
        a[i] = 0;
    }

    int k=__VERIFIER_nondet_int();
    __VERIFIER_assume(i0 <= k && k < n);

    if(a[k] != 0)
        __VERIFIER_error();
}
