// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int VERIFIER_nondet_int();

int main() {
  int x = VERIFIER_nondet_int();
  if (x < 0) {
    return 1;
  }
  int k = 5;
  int y = x - k;
  while (x > y) {
    y++;
  }
  return 1;
}
