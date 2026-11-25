// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The error check is several blocks away from the root. No block has a precondition when the
// violation condition is created, so only the speculative exploration from the unconstrained state
// carries it back to the root. Without that exploration, DSS answers TRUE.

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "speculation_reaches_root_unsafe.c", 3, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int lock = 0;

int main() {
  lock = 1;
  while (__VERIFIER_nondet_int()) {
  }
  while (__VERIFIER_nondet_int()) {
  }
  while (__VERIFIER_nondet_int()) {
  }
  if (lock != 0) {
    reach_error();
    abort();
  }
  return 0;
}
