#include<assert.h>

int a = 0;

#define BOUND 100

void inc_a() {
    a++;
}

int main() {
    for (int i=0; i<BOUND; i++) {
        inc_a();
    }
    assert(a == BOUND);
}
