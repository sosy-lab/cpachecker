// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The condition edges of the inner if statement are also part of the condition of the outer one.
extern void reach_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  int y = 0;
  if (({ int t = 0; if (x > 5) t = 1; t; })) y = 1;
  if (y) reach_error();
  return 0;
}
