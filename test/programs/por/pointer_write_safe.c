// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A thread writes a global THROUGH A POINTER, and main reads the global after
// joining. One writer, joined before the read, so shared is 1 and the error is
// unreachable: safe.
//
// This is the regression guard for the counterexample check. The wrapped value
// analysis does not learn `p == &shared`, so `*p = 1` leaves `shared` unknown to
// it, both directions of the later assume stay open, and it hands up a spurious
// counterexample. That is fine and expected — an imprecise wrapped domain is
// allowed to propose infeasible error paths, and refuting them is exactly what
// the counterexample check is for.
//
// What is NOT fine is confirming one. The POR cex-check config used to set
// `cpa.predicate.blk.threshold = 1`; since CEGAR is off in a cex check the
// predicate set is always empty, so that forced a "true" abstraction after every
// single edge, discarding the path formula. The checker thus retained no facts at
// all — it could not even see that `*p = 1` implies `shared == 1` one edge later —
// and rubber-stamped the spurious path, turning this safe program into a wrong
// FALSE (an incorrect-false, the most heavily penalised SV-COMP verdict).
//
// Hence the assertion for this file is "never FALSE" rather than a fixed verdict:
// the predicate configs prove it TRUE, while the value configs may honestly answer
// UNKNOWN (they cannot prove safety, but they must not claim a violation).

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int shared;
int *p = &shared;

void *writer(void *arg) {
  *p = 1;
  return 0;
}

int main() {
  pthread_t t;
  shared = 0;
  pthread_create(&t, 0, writer, 0);
  pthread_join(t, 0);
  if (shared != 1) {
    reach_error();
    abort();
  }
  return 0;
}
