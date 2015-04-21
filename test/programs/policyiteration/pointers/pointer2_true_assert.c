#include <assert.h>
int main()
{
    int a;
    int* p;
    int* q;
    a = 1;
    p = &a;
    q = p;
    *q = 2;
    assert(a == 2);
    return 0;
}
