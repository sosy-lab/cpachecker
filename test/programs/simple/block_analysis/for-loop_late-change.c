// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
void reach_error(){}

int main() {
  int x = __VERIFIER_nondet_int();
  if (x > 0 && x < 100) {
    for (int i = 0; i < 7; i++) {
      x++;
      if (i == 6) x--;
    }
  } else {
    x = -1;
  }
  if (x >= 105) reach_error();
}
