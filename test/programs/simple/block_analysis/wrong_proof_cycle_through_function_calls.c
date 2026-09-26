// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
void reach_error(void) {}

int cmp2(int a, int b) {
  if (a < b) {
    return -1;
  }
  return 0;
}

int main(void) {
  int a = __VERIFIER_nondet_int();
  int ab = cmp2(a, 0);
  int ba = cmp2(0, a);
  if (ab < 0 && ba < 0) {
    reach_error();
  }
  reach_error();
  return 0;
}
