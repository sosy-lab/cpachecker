// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void *malloc(unsigned long);

int main() {
  int *p = malloc(sizeof(int));
  if (!p) {
    return 0;
  }
  *p = __VERIFIER_nondet_int();
  if (*p > 0) {
    *p = 0;
    if (*p == 0) {
ERROR:
      return -1;
    }
  }
  return 0;
}
