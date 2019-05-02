#include<assert.h>
void method_with_assert() {
    int c = 0;
    assert(c > 0);
}

int main() {
    method_with_assert();
}
