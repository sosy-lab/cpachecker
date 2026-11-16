// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Overflow check: the overflow happens in main, AFTER a pthread_join.
//
// This is the regression guard for a POR/OverflowCPA interaction that silently
// swallowed everything past a join. OverflowTransferRelation used to build its
// successors entirely inside `for (nextEdge : cfaEdge.getSuccessor().getLeavingEdges())`,
// so at a node with no leaving edges it returned an EMPTY collection. A thread's
// last edge ends at its function exit node, which has no leaving edges — so the
// thread's final state was killed, the thread never reached its exit, it never
// counted as terminated, `canJoin` was never satisfied, and the pthread_join here
// was never enabled. main simply stalled at the join: `x = x + 1` below was never
// explored at all, and the analysis reported a confident, wrong TRUE.
//
// (The same one-line bug also loses a plain sequential overflow committed on the
// last edge of a function, e.g. `return x + 1;`, whose successor is the exit node.)
//
// Expected verdict: FALSE.

#include <pthread.h>
#include <limits.h>

int x = INT_MAX;

void *noop(void *arg) {
  return 0;
}

int main() {
  pthread_t t;
  pthread_create(&t, 0, noop, 0);
  pthread_join(t, 0);
  x = x + 1;
  return 0;
}
