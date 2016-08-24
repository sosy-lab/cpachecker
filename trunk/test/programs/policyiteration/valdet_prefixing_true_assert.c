#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);

int main() {
    int x=0, y=0, z=0;
    while (__VERIFIER_nondet_int()) {
        x = __VERIFIER_nondet_int();
        __VERIFIER_assume(x>=-10 && x <= 10);

        while (__VERIFIER_nondet_int()) {
            // cutpoint.
        }

        if (x == 1) {
            y = x;
        } else if (x == -1) {
            z = -x;
        }
    }
    assert(x + y + z == x + y + z);
}
