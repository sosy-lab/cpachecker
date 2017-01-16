#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int expression);

int sum(int param) {
    if (param == 0) {
        return param;
    }
    return param + sum(param - 1);
}

int main() {
    int x = __VERIFIER_nondet_int();
    __VERIFIER_assume(x >= 0);
    int out = sum(x);
    assert(out >= 0);
    assert(out >= x);
}
