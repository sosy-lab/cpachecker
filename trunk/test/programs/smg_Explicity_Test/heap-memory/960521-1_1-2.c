// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
