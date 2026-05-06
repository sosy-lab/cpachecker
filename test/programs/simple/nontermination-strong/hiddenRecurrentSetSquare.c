// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 CPAchecker contributors
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
  int x = __VERIFIER_nondet_int();

  while (x < 10) {
    x = x * x;
  }

  return 0;
}
