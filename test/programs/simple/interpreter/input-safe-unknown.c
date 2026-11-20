// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// No input violates the specification, but an analysis that samples one assignment of the input
// explores only one of the executions of the program and must therefore report UNKNOWN.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x > 0 && x < 0) {
    reach_error();
  }
  return 0;
}
