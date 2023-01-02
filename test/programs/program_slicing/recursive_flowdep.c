// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int fac(int n) {
  if (n <= 0) {
    __VERIFIER_error();
  }
  // misses statement
  // if (n == 1) return 1;
  return n + fac(n - 1);
}

int main() {
  int x = 4 + 6;
  fac(x);
}
