#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);


int pong(int a, int b) {
    assert(a + b >= 0);
    return ping(b + 1, a + 1);
}

int ping(int a, int b) {
    assert(a + b >= 0);
    return pong(b + 1, a - 1);
}

int main() {
    int a = __VERIFIER_nondet_int();
    int b = __VERIFIER_nondet_int();
    __VERIFIER_assume(a >= 0);
    __VERIFIER_assume(b >= 0);
    int out = ping(a, b);
}
