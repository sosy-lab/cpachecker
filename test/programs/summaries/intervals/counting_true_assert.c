#include<assert.h>

extern int __VERIFIER_nondet_int();

int a = 0;

void f(int param) {
    a++;
    if (param == 0) {
        return;
    }
    f(param - 1);
}

int main() {
    f(5);
    assert(a == 5);
}
