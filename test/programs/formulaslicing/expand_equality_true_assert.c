#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
    int i = 1;
    for (int k=0; k<__VERIFIER_nondet_int(); k++) {
        i++;
    }
    assert(i >= 0);
}
