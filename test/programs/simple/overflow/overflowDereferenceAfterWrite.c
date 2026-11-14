// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
extern void *malloc(unsigned long);

int main(void) {
  int *p = malloc(sizeof(int));
  if (!p) {
    return 0;
  }
  *p = __VERIFIER_nondet_int();
  if (*p > 0) {
    *p = __VERIFIER_nondet_int();
    return *p - 1; // overflow for *p == INT_MIN
  }
  return 0;
}
