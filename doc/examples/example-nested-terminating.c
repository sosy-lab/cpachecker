// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();

  while (x > 0) {
    y = 0;
    while (y < x) {
        y = y + 1;
    }
    x = x - 1;
  }
}