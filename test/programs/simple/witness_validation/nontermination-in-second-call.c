// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

void count_down(int n) {
  while (n > 0) {
    if (n != 3) {
      n = n - 1;
    }
  }
}

int main() {
  count_down(1);
  count_down(__VERIFIER_nondet_int());
  return 0;
}
