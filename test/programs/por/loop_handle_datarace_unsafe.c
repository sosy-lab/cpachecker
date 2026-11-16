// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The sibling of loop_handle_datarace_safe.c: a thread pool with array-of-handles
// created and joined in loops, but here main reads `data` BEFORE the join loop, while
// the worker threads may still be running and writing it. That read races with the
// unprotected write in `thread`, so the program is UNSAFE. This is the guard that the
// symbolic-index handle path still detects a genuine race (it must stay FALSE); the
// safe sibling's spurious FALSE is the known-false-alarm being tracked separately.

#include <pthread.h>

int data = 0;

void *thread(void *arg) {
  data = 1; // RACE: unsynchronized write
  return 0;
}

int main() {
  pthread_t tids[2];
  for (int i = 0; i < 2; i++) {
    pthread_create(&tids[i], 0, thread, 0);
  }
  int r = data; // RACE: read while the workers may still be writing
  for (int i = 0; i < 2; i++) {
    pthread_join(tids[i], 0);
  }
  return r;
}
