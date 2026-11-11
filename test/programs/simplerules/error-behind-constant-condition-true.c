// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The condition in front of the error is never true: the global variable is never assigned.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int enabled = 0;

int main(void) {
  int x = __VERIFIER_nondet_int();
  if (enabled) {
    reach_error();
  }
  if (x == 42) {
    return 1;
  }
  return 0;
}
