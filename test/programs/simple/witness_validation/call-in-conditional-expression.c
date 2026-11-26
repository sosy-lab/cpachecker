// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int f(int x) {
  return x + 1;
}

int main() {
  int b = __VERIFIER_nondet_int();
  int r = b ? 0 : f(b);
  return r;
}
