// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

struct ops {
  int (*lookup)(int);
};

int increment(int x) {
  return x + 1;
}

// This program dereferences a function pointer and uses the result as a value
int main() {
  struct ops o;
  struct ops *p = &o;
  o.lookup = &increment;

  int n = __VERIFIER_nondet_int();

  if ((long long)(*(p->lookup)) != 0LL) {
    if (n == 42) {
      goto ERROR;
    }
  }
  return 0;

  ERROR: // Reached whenever the function pointer is non-null and n equals 42
  return 1;
}
