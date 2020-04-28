extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern void __VERIFIER_assume(int cond) { if(!cond) while(1){} }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: __VERIFIER_error(); } }
extern int __VERIFIER_nondet_int();

int main() {
    unsigned n=__VERIFIER_nondet_int();
    //unsigned n=__VERIFIER_nondet_unsigned();
    __VERIFIER_assume(0 < n);
    int a[n];

    unsigned i=__VERIFIER_nondet_int();
    //unsigned i=__VERIFIER_nondet_unsigned();
    __VERIFIER_assume(0 <= i && i < n);
    //__VERIFIER_assume(i < n);

    for(unsigned j=i; j<n; j++) {
        a[j] = 0;
    }

    unsigned k=__VERIFIER_nondet_int();
    //unsigned k=__VERIFIER_nondet_unsigned();
    __VERIFIER_assume(i <= k && k < n);

    if(a[k] != 0)
        __VERIFIER_error();
}
