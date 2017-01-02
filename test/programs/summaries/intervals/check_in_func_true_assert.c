#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);

f(int param) {
    assert(param > 0);
    f(param + 1);
}

int main() {
    int input = __VERIFIER_nondet_int();
    __VERIFIER_assume(input > 0);
    f(input);
}
