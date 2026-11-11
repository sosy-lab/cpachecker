// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A thread overflows a shared signed int; a second thread just runs
// concurrently so this exercises the overflow property in a threaded setting.
//
// This is the regression guard for the second of two POR/OverflowCPA wrong-TRUE bugs (the first is
// in overflow_after_join_unsafe.c). POR applies synthetic bookkeeping edges of its own: the handle
// write at a pthread_create, the handle-equality assume at a join. Feeding an already-VIOLATING
// state through one of them returned an empty successor collection — and OverflowCPA reports a
// violation precisely by producing no successors, which POR then read as "infeasible branch" and
// dropped. See ConcurrentTransferRelation#applyBookkeepingEdge. The second pthread_create below is
// the edge that destroyed the flagged state.
//
// Expected verdict: FALSE.
#include <pthread.h>
#include <limits.h>

int x = INT_MAX;

void *overflower(void *arg) {
  x = x + 1;
  return 0;
}

void *other(void *arg) {
  int y = 0;
  y++;
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, overflower, 0);
  pthread_create(&t2, 0, other, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
