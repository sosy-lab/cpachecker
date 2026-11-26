// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// An atomic block contains a loop. Collecting the nodes of the atomic block must terminate.

#include <pthread.h>

extern void reach_error(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int x;

void *thread(void *arg) {
  __VERIFIER_atomic_begin();
  for (int i = 0; i < 2; i++) {
    x = x + 1;
  }
  __VERIFIER_atomic_end();
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, thread, 0);
  pthread_join(t, 0);
  if (x != 2) {
    reach_error();
  }
  return 0;
}
