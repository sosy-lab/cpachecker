#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);

int f(int param) {
    if (param < 0) {
        return -1;
    }
    if (__VERIFIER_nondet_int()) {
        return param;
    }
    return param + f(param + 1);
}

int main() {
    int p = __VERIFIER_nondet_int();
    __VERIFIER_assume(p >= 0);
    int out = f(p);
    assert(out >= 0);
}
