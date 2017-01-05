#include<assert.h>

extern int __VERIFIER_nondet_int();

#define BOUND 100

int a = 0;

void f(int param) {
    a++;
    if (param == 0) {
        return;
    }
    f(param - 1);
    f(param - 1);
}

int main() {
    f(BOUND);
    assert(a == BOUND);
}
