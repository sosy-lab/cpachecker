// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Both additions can overflow. An analysis that does not stop at the first violation therefore
// has two target states, and the counterexample of the second one passes the ARG branching that
// the OverflowCPA creates for the first one.

extern int __VERIFIER_nondet_int(void);

int main() {
  int a = __VERIFIER_nondet_int();
  int b = a + 1;
  return b + 1;
}
