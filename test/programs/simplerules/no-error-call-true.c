// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program does not contain a call of reach_error(), so no execution can call it.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  int y = x + 1;
  if (y > x) {
    return 0;
  }
  return 1;
}
