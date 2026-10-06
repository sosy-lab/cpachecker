// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The input is read outside of every loop, so one assignment of it is enough to execute the
// program. Finding the violation needs an assignment that leaves the program through the second
// branch of the condition.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (x != 42) {
    return 0;
  }
  reach_error();
  return 0;
}
