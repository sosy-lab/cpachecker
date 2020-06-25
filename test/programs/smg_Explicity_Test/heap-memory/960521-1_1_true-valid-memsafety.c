extern void __VERIFIER_error() __attribute__ ((__noreturn__));

#include <stdlib.h>

int *a, *b;
int n;

extern int __VERIFIER_nondet_int(void);

void foo()
{
  int i;
  for (i = 0; i < n; i++)
    a[i] = n;
  for (i = 0; i < n - 1; i++)
    b[i] = n;
}

int main()
{
  n = 1;

  while(__VERIFIER_nondet_int() && n < 3) {
    n++;
  }

  a = malloc (n * sizeof(*a));
  b = malloc (n * sizeof(*b));

  *b++ = n;
  foo ();

  if (b[-1] - b[n - 2])
  { free(a); free(b); } /* invalid, but branch not accessible */
  else
  { free(a); free(b-1); }
  return 0;
}
