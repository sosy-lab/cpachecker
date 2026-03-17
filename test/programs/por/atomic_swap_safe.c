// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads swap two shared variables inside atomic blocks.
// The sum x + y is invariant (always 10). This is SAFE because each
// swap is fully atomic.

#include <pthread.h>

extern void reach_error(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int x = 3;
int y = 7;

void *t1(void *arg) {
  __VERIFIER_atomic_begin();
  int tmp = x;
  x = y;
  y = tmp;
  __VERIFIER_atomic_end();
  return NULL;
}

void *t2(void *arg) {
  __VERIFIER_atomic_begin();
  int tmp = x;
  x = y;
  y = tmp;
  __VERIFIER_atomic_end();
  return NULL;
}

int main() {
  pthread_t id1, id2;
  pthread_create(&id1, NULL, t1, NULL);
  pthread_create(&id2, NULL, t2, NULL);
  pthread_join(id1, NULL);
  pthread_join(id2, NULL);
  if (x + y != 10) { ERROR: {reach_error();abort();} }
  return 0;
}
