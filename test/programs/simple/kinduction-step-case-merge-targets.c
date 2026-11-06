// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The error is reachable: the innermost loop can decrement b below 0.
//
// k-induction reported TRUE for this program at k = 1 when its step case merged
// target states (cf. #1694): in BMC mode, target states are predicate
// abstraction states, which cover each other regardless of their block
// formulas, so merging two of them dropped the paths of one of them, and with
// them the counterexamples to induction.

extern int __VERIFIER_nondet_int(void);
extern void reach_error(void);

int main(void) {
  int a = 2;
  int b = 2;
  int c = 1;
  while (b < 6) {
    while (c < 3) {
      while (__VERIFIER_nondet_int()) {
        a = b;
        c = c + 2;
        if (__VERIFIER_nondet_int()) { b = b + 1; } else { c = 0; }
        if (__VERIFIER_nondet_int()) { b = b + 1; } else { b = b - 1; }
      }
    }
    if (__VERIFIER_nondet_int()) { b = b + 1; } else { c = 0; }
    if (!(b >= 0)) { reach_error(); }
  }
  if (__VERIFIER_nondet_int()) { c = c + 1; } else { a = 0; }
  return 0;
}
