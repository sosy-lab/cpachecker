#include<assert.h>

extern int __VERIFIER_nondet_int();

void g() {
    // Force an abstraction.
    for (int i=0; i<10; i++) { }
}

void f() {
    int a = 5;
    g();
    int b = a;
    assert(b == 5);
}

int main() {
    f();
}
