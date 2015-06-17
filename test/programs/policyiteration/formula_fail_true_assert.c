#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
    int x = 0;
    while (__VERIFIER_nondet_int()) {
        x = 2;
    }
    assert(x <= 2);
}
