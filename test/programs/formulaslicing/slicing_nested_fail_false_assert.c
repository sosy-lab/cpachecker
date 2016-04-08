#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
    while (__VERIFIER_nondet_int()) {

        int a = 0;
        for (int j=0; j<10; j++) { }

        for (int j=0; j<10; j++) {
            a++;
        }
        assert(a == 0);
    }
}
