// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

void check(int cond) {
  if (!cond) {
    abort();
  }
}

int main() {
  int n = __VERIFIER_nondet_int();
  if (n < 0 || n > 100) {
    return 0;
  }
  int i = 0;
  while (1) {
    check(i <= n);
    if (i >= n) {
      break;
    }
    i = i + 1;
  }
  return i;
}
