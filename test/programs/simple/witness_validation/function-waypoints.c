// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
void reach_error() {}

int inc(int x) { return x + 1; }

int main() {
  int x = __VERIFIER_nondet_int();
  int y = inc(x);
  if (y == 10) {
    reach_error();
  }
  return 0;
}
