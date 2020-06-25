#include<assert.h>

int main() {
        int j = 42;
        int *p = &j;
        int i = (int)*p;
        assert(i == 42);
        assert(!(i != 42));
        return 0;
}
