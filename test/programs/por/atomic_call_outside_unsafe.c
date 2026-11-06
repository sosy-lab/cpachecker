// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// id() is called inside and outside an atomic block. The statements after the call outside the
// atomic block are not atomic, so the thread can read g == 1 between the two writes.

#include <pthread.h>

extern void reach_error(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int g;
int x;

int id(int a) { return a; }

void *thread(void *arg) {
  if (g == 1) {
    reach_error();
  }
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, thread, 0);
  __VERIFIER_atomic_begin();
  x = id(0);
  __VERIFIER_atomic_end();
  x = id(0);
  g = 1;
  g = 2;
  pthread_join(t, 0);
  return 0;
}
