// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The thread writes a[k] at a symbolic, nondeterministically chosen index.
// When k happens to be 3, the write hits a[3] and the check in main fires.
// Exercises symbolic array indexing: the rf/ws relation between the write
// and the read of a[3] must be guarded by an offset-equality constraint.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);
extern int __VERIFIER_nondet_int(void);

int a[8];
int k;

void *writer(void *arg) {
  a[k] = 5;
  return 0;
}

int main() {
  k = __VERIFIER_nondet_int();
  if (k < 0 || k >= 8) {
    return 0;
  }
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (a[3] == 5) { ERROR: { reach_error(); abort(); } }
  return 0;
}
