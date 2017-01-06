#include<assert.h>

int a = 0;

void f(int param) {
    a++;
    if (param == 0) {
        return;
    }
    f(param - 1);
}

int main() {
    f(4);
    assert(a == 5);
}
