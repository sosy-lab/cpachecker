// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned int size_t;
extern void *alloca(size_t size);
extern int __VERIFIER_nondet_int(void);

// Like inner-constant-bound, but the variables are stored in memory allocated with alloca
int main() {
  int *n = alloca(sizeof(int));
  int *i = alloca(sizeof(int));
  int *j = alloca(sizeof(int));
  *n = __VERIFIER_nondet_int();
  *i = 0;
  while (*i < *n) {
    *j = 0;
    while (*j < 3) {
      *j = *j + 1;
    }
    *i = *i + 1;
  }
  return 0;
}
