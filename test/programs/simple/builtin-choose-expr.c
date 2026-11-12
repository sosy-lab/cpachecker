// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Only the argument chosen by the constant condition is evaluated,
// the other one may have type void.
void bad_size(void) {}

int get16(int v) { return v; }

int main(void) {
  int x = 2;
  int r = __builtin_choose_expr(sizeof(short) == 2, get16(x), bad_size());
  int s = __builtin_choose_expr(sizeof(short) == 4, bad_size(), x + 1);
  __builtin_choose_expr(1, x++, bad_size());
  if (r != 2 || s != 3 || x != 3) {
  ERROR:
    return 1;
  }
  return 0;
}
