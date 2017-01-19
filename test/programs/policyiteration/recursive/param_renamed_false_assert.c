#include<assert.h>

extern int __VERIFIER_nondet_int();

int f(int param) {
    param += 1;
    return param;
}

int main() {
    int a = __VERIFIER_nondet_int();
    int b = f(a);
    assert(b == a);
}
