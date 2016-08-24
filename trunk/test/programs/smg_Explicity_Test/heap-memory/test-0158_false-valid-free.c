extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>
#include <string.h>

int main()
{
    union {
        void *p0;

        struct {
            char c[2];
            void *p1;
            void *p2;
        } str;

    } data;

    // alloc 37B on heap
    data.p0 = malloc(37U);

    // this should be fine
    data.str.p2 = &data;

    // avoid introducing a memleak
    void *ptr = data.p0;

    // invalid free()
    data.str.c[1] = sizeof data.str.p1;
    free(data.p0);

    free(ptr);
    return 0;
}
