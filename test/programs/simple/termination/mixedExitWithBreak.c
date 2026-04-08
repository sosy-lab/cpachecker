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

  if (x < 0) {
    x = 0;
  }
  if (x > 1) {
    x = 2;
  }

  while (x != 1) {
    if (x > 0) {
      break;
    }
    x++;
  }

  return 0;
}
