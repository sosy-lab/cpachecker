// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Both branches of the if-statement call pthread_create with the same
// target (adder), so they must be recognized as ONE thread instance,
// not two. The unconditional pthread_create afterwards creates a
// genuine second instance, so at most two increments of x can happen.

#include <pthread.h>

extern void abort(void);
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int x = 0;

void *adder(void *arg) {
  x = x + 1;
  return 0;
}

int main() {
  pthread_t t1, t2;
  if (__VERIFIER_nondet_int()) {
    pthread_create(&t1, 0, adder, 0);
  } else {
    pthread_create(&t1, 0, adder, 0);
  }
  pthread_create(&t2, 0, adder, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  if (x > 2) { ERROR: {reach_error();abort();} }
  return 0;
}
