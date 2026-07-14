// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// main has a LOCAL `x`, and passes `&x` to a thread that writes through the pointer. The local
// therefore escapes into the other thread and must be modeled as one shared object across both:
// the thread's `*arg = 1` and main's `x` are the same memory. main joins the thread, so after the
// join `x == 1` and the error is unreachable. SAFE.
//
// This guards the escaping-local base binding: if the thread's write did not alias main's local,
// main would see `x == 0`, take the error branch, and the analysis would (wrongly) report FALSE.

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
  if (x != 1) {
    ERROR: {reach_error(); return 1;}
  }
  return 0;
}
