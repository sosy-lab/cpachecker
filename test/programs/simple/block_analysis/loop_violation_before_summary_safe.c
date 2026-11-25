// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The violation condition lock != 0 reaches the loop block before the loop block has published
// its own summary. The loop block is its own predecessor, so it explores speculatively from the
// unconstrained state, where lock != 0 is possible. DSS used to withhold the summary lock == 0
// computed from the real precondition in this case. The loop block then never received its own
// summary and extended the violation condition by one iteration of x++ forever.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "loop_violation_before_summary_safe.c", 3, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int lock = 0;

int main() {
  int x = 0;
  while (x < 1000) {
    if (__VERIFIER_nondet_int()) {
      break;
    }
    x++;
  }
  if (lock != 0) {
    reach_error();
    abort();
  }
  return 0;
}
