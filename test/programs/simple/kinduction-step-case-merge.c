// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The error is reachable: the innermost loop can increment a past 10.
//
// k-induction reported TRUE for this program at k = 2 when its step case merged
// states with different per-loop iteration counts and kept the counts of only
// one of the merged states (cf. #1694): the paths of the other one got wrong
// iteration numbers and counterexamples to induction got lost. For small k, they all
// start at an inner loop head with a > 10, because a is at most 5 after the
// reset at the start of the outer loop.

extern int __VERIFIER_nondet_int(void);
extern void reach_error(void);

int main(void) {
  int a = 2;
  int b = 0;
  int c = 2;
  while (__VERIFIER_nondet_int()) {
    if (a > 5) {
      a = 0;
    }
    while (b < 8) {
      while (__VERIFIER_nondet_int()) {
        c = 2;
        a = a + 1;
      }
      b = b + 2;
    }
    if (!(a <= 10)) {
      reach_error();
    }
  }
  return 0;
}
