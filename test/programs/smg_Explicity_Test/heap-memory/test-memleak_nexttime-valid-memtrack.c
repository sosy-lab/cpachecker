extern void __VERIFIER_error() __attribute__ ((__noreturn__));


/* Contributed by Kamil Dudka. */

#include <stdlib.h>
#include <string.h>

char a[sizeof(int*)];

void foo(void)
{
   int *p = (int *)malloc(10);
   memcpy(a, &p, sizeof p);
}

int main(void)
{
   foo();
   void *p;
   memcpy(&p, a, sizeof p);
   free(p);
}

