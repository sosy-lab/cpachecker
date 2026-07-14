// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The sibling of escaping_local_safe.c: main's local `x` escapes to a thread that writes it
// through `&x`. After the join `x == 1`, and here the error is on exactly that value, so it IS
// reachable. UNSAFE.
//
// This guards the same escaping-local binding from the other side: if the thread's write did not
// alias main's local, main would see `x == 0`, the error would look unreachable, and the analysis
// would (wrongly) report TRUE — a missed bug.

#include <pthread.h>

extern void reach_error(void);

void *writer(void *arg) {
  *(int *)arg = 1;
  return 0;
}

int main() {
  int x = 0;
  pthread_t t;
  pthread_create(&t, 0, writer, &x);
  pthread_join(t, 0);
  if (x == 1) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
