extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

int main()
{
    union {
        void *p0;

        struct {
            char c[2];
            int  p1;
            int  p2;
        } str;

    } data;

    // alloc 37B on heap
    data.p0 = malloc(37U);

    // avoid introducing a memleak
    void *ptr = data.p0;
    
    // this should be fine
    if(__VERIFIER_nondet_int()) {
      data.str.p2 = 20;
    } else {
      data.str.p2 = 30;
    }
    
    if(25 > data.str.p2) {
       // avoids memleak
       data.str.c[1] = sizeof data.str.p1;
    }

    // invalid free()
    free(data.p0);

    free(ptr);
    return 0;
}
