// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc(unsigned long int);
extern _Bool __VERIFIER_nondet_bool();

int f() {
  int x;
  // needs address taken
  *(&x) = 0;
  return x;
}

int main() {
  int* p = malloc(4);
  if (p == 0) {
    return 0;
  }
  *p = 1;

  if (__VERIFIER_nondet_bool()) {
    if (f() != 0) {
      return 0;
    }
  }

  if (f() != 0) {
    goto ERROR;
  }

  if (*p == 1) {
    return 0;
  }
ERROR:
  return 1;
}
