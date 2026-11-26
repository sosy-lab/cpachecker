// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads each increment a shared counter inside atomic blocks.
// The final value must be 2. This is SAFE because the atomic blocks
// prevent any interleaving of the read-modify-write sequences.

#include <pthread.h>

extern void reach_error(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int counter = 0;

void *thread(void *arg) {
  __VERIFIER_atomic_begin();
  counter = counter + 1;
  __VERIFIER_atomic_end();
  return NULL;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, NULL, thread, NULL);
  pthread_create(&t2, NULL, thread, NULL);
  pthread_join(t1, NULL);
  pthread_join(t2, NULL);
  if (counter != 2) { ERROR: {reach_error();abort();} }
  return 0;
}
