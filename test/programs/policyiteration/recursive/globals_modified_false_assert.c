#include<assert.h>

extern int __VERIFIER_nondet_int();

int a = 0;

int f(param) {
    a = param;
    while (__VERIFIER_nondet_int()) {
        f(param + 1);
    }
}

int main() {
    f(10);
    assert(a == 10);
}
