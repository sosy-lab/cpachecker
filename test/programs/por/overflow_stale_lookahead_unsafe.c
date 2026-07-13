// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// KNOWN BUG, currently reported as TRUE by por-value-overflow and
// por-pred-overflow. Deliberately NOT wired into PORCPAOverflowTest, which would
// go red; it is kept here as the reproducer, ready to enable once fixed.
//
// The program overflows: `setter` runs to completion, then `incr` computes
// INT_MAX + 1. Independently confirmed with POR's (well-tested) reachability
// analysis -- asserting `y == INT_MAX` at the point of the increment is reported
// FALSE, i.e. that state is reachable -- while the overflow analysis on this same
// program answers TRUE. A missed overflow.
//
// Why: OverflowCPA does not check the edge it is given. To constrain `y + 1` it
// needs y's value BEFORE the increment, so it looks AHEAD: while processing some
// edge E it inspects E.getSuccessor().getLeavingEdges(), builds the no-overflow
// assumption for the next statement, and parks a flag on the state. The overflow
// then surfaces one edge later. That is sound sequentially, because the next edge
// really is the next thing that runs.
//
// Under POR it is not, because the next edge that runs may belong to ANOTHER
// THREAD. Concretely, the lookahead for `y = y + 1` is computed while `incr`
// executes the assume edge of the `if` below. At that moment y is still 0, so the
// flag is set to "no overflow". `setter` then writes INT_MAX, and `incr` finally
// executes `y = y + 1` carrying the now-stale flag. The overflow is never reported.
//
// The interleaving that WOULD have caught it -- setter's write before incr's
// assume edge, so the lookahead sees y == INT_MAX -- is pruned by POR's reduction:
// the assume reads only the local `local`, so POR's dependency analysis (which
// looks at the edge's own defs/uses) calls it independent of setter's write to y
// and explores just one of the two orders. But the two orders are NOT equivalent,
// because OverflowCPA's lookahead makes that edge read y after all. POR's
// independence relation does not know about the lookahead.
//
// The dependence on this being invisible to POR is exact: change the guard to
// `if (y >= 0)` -- same program otherwise -- and the assume now reads the global,
// POR calls it dependent, explores both orders, and correctly reports FALSE.
//
// Possible fixes: (a) have POR's dependency relation also count, as uses of an
// edge, the uses of the leaving edges of its successor node (sound, but weakens
// the reduction for every POR analysis, so ideally only when an OverflowCPA is in
// the composite); or (b) restructure OverflowCPA so the overflow condition is
// evaluated against the state the edge is actually taken from, rather than
// predicted one edge in advance.

#include <pthread.h>
#include <limits.h>

int y = 0;

void *setter(void *arg) {
  y = INT_MAX;
  return 0;
}

void *incr(void *arg) {
  int local = 0;
  if (local == 0) { /* thread-local guard: POR calls this edge independent of setter */
    y = y + 1;      /* overflows iff setter already ran */
  }
  return 0;
}

int main() {
  pthread_t a, b;
  pthread_create(&a, 0, incr, 0);
  pthread_create(&b, 0, setter, 0);
  pthread_join(a, 0);
  pthread_join(b, 0);
  return 0;
}
