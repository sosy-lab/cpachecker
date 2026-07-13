// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A thread pool with array-of-handles created and joined in loops (symbolic index
// `tids[i]`). Every write to `data` is mutex-protected, and main reads `data` only
// AFTER joining every thread, so the program is race-free.
//
// KNOWN FALSE ALARM (ordering consistency): OC reports a data race here. A join of a
// symbolic-index handle branches over the live thread candidates and orders exactly one
// of them before the join; because the value read back from `tids[i]` is not tied to the
// id written at `pthread_create(&tids[i], ...)` (the `&tids[i]` write and the `tids[i]`
// read record their base/offset terms asymmetrically, so no read-from pair constrains
// them), the branch in which both loop iterations resolve to the SAME candidate survives,
// leaving the other thread unordered before main's read. With a literal index throughout
// (see two_threads / array-handle tests) the fast-path key resolves each handle exactly
// and this does not happen. Two threads is the minimum that triggers it.

#include <pthread.h>

int data = 0;
pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;

void *thread(void *arg) {
  pthread_mutex_lock(&m);
  data = data + 1; // NORACE: mutex-protected
  pthread_mutex_unlock(&m);
  return 0;
}

int main() {
  pthread_t tids[2];
  for (int i = 0; i < 2; i++) {
    pthread_create(&tids[i], 0, thread, 0);
  }
  for (int i = 0; i < 2; i++) {
    pthread_join(tids[i], 0);
  }
  return data; // NORACE: every thread has been joined
}
