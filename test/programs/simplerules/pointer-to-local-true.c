// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program uses a pointer, but it never allocates memory, so there is no block that could be
// freed invalidly, be leaked, or still be allocated when the program ends.
extern int __VERIFIER_nondet_int(void);

int main(void) {
  int x = __VERIFIER_nondet_int();
  int *p = &x;
  *p = *p + 1;
  return x;
}
