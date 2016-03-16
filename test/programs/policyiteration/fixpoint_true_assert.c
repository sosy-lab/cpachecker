#include<assert.h>

extern int __VERIFIER_undefined_int();

int main() {
    float x = 0;
    while (__VERIFIER_undefined_int()) {
        x = x / 2 + 1;
    }
    assert(x <= 2);
}
