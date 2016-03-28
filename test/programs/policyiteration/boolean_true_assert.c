#include<assert.h>
#include<stdbool.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);

#define BOUND 1000

int main () {
    int test;
    if (__VERIFIER_nondet_int()) {
        test = 0;
    } else {
        test = 1;
    }
    int sum = 0;
    for (int i=0; i<BOUND; i++) {
        if (test) {
            sum++;
        }
    }
    assert(sum == BOUND || sum == 0);
}
