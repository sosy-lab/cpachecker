#include<assert.h>

extern int __VERIFIER_nondet_int();

int a = 0;

#define BOUND 100

void inc_a() {
    if (__VERIFIER_nondet_int()) {
        a++;
    }
}

int main() {
    for (int i=0; i<BOUND; i++) {
        inc_a();
    }
    assert(a == BOUND);
}
