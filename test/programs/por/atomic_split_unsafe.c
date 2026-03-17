// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads increment a shared counter, but the read and write are in
// SEPARATE atomic blocks. This allows a lost-update race: both threads
// can read the same value before either writes. The final counter can
// be 1 instead of 2, so the assertion can fail. This is UNSAFE.

#include <pthread.h>

extern void reach_error(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int counter = 0;

void *thread(void *arg) {
  __VERIFIER_atomic_begin();
  int tmp = counter;
  __VERIFIER_atomic_end();
  // <-- other thread can interleave here
  __VERIFIER_atomic_begin();
  counter = tmp + 1;
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
