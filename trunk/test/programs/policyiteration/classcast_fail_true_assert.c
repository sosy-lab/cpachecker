#include<assert.h>

extern int __undef();
extern int __undef2();

int main() {
    int x = 0;
    while (__undef()) {
        x = 1;
        while (__undef2()) {
            x = x + 3;
            if (x == 4) {break;}
        }
    }
    assert(x >= 0 && x <= 4);
}
