#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();
extern void __VERIFIER_assume(int condition);

int fib(int n);

int main() {
    int x = __VERIFIER_nondet_int();
    int out = fib(x);
    assert(out >= x);
}

int fib(int n) {
    if (n < 2) {
        return n;
    } else {
        return fib(n-1) + fib(n-2);
    }
}
