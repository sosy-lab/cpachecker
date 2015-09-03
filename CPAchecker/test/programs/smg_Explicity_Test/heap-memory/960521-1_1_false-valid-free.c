extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

int *a, *b;
int n;

extern int __VERIFIER_nondet_int(void);

void foo()
{
  int i;
  for (i = 0; i < n; i++)
    a[i] = -1;
  for (i = 0; i < n - 1; i++)
    b[i] = -1;
}

int main()
{
  n = 1;

  while(__VERIFIER_nondet_int() && n < 3) {
    n++;
  }

  a = malloc(n * sizeof(*a));
  b = malloc(n * sizeof(*b));

  *b++ = 0;
  foo();

  if (b[-1])
  { free(a); free(b); } /* invalid free (b was iterated) */
  else
  { free(a); free(b); } /* ditto */

  return 0;
}
