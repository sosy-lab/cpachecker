#include<assert.h>

#define BOUND 100

int a = 0;

void f(counter) {
    if (counter < 0) {
        return;
    } else {
        a++;
        f(counter - 1);
    }
}

int main() {
    f(BOUND);
    assert(a == BOUND);
}
